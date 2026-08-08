package mukkeu.mukkeu.analysis.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import mukkeu.mukkeu.analysis.dto.AnalysisRequest;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.Candidate;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.DishResult;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.ExactMatch;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.ItemResponse;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.OptionResponse;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.StoreResponse;
import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.menu.domain.Menu;
import mukkeu.mukkeu.menu.domain.MenuMatch;
import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.menu.app.OptionMatcher;
import mukkeu.mukkeu.menu.domain.repository.MenuRepository;
import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.domain.repository.RestaurantRepository;
import mukkeu.mukkeu.user.domain.User;
import mukkeu.mukkeu.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 영상 분석 → 내 근처에서 시킬 수 있는 매장·메뉴.
 *
 * 두 경로로 찾는다.
 *   Path A  brandName 이 맞는 지점을 찾는다        → exactMatches
 *   Path B  요리별로 맛이 비슷한 메뉴를 벡터로 찾는다 → dishResults
 *
 * Path B 는 요리 하나씩 따로 검색한다.
 *   여러 요리를 한 질의문으로 합치면 임베딩이 그 평균이 되어, 떡볶이도 치킨도
 *   아닌 어중간한 벡터가 만들어진다. 실제로 "떡볶이 + 후라이드" 요청에
 *   토스트와 돈까스가 올라왔다. 요리별로 나누면 각 벡터가 선명해지고,
 *   옵션도 그 요리 것만 적용되어 "치즈 추가" 가 엉뚱한 메뉴에 켜지지 않는다.
 *
 * 어떤 조건도 서버가 임의로 완화하지 않는다.
 *   맵기·고기는 "먹을 수 있나" 를 묻는 조건이고, 배달시간을 몰래 늘리면
 *   화면이 버그처럼 보이며, 반경을 넓히면 배달 안 되는 집이 뜬다.
 *   결과가 0개면 0개로 돌려주고 사용자가 직접 풀게 한다.
 *
 * 트랜잭션을 걸지 않는다.
 *   벡터 검색이 DBMS_VECTOR.UTL_TO_EMBEDDING 으로 OCI 에 HTTP 를 친다.
 *   그 왕복(수백 ms) 내내 커넥션을 잡고 있으면 동시 요청 시 풀이 마른다.
 *   여러 조회 사이에 일관성이 필요한 것도 아니라 읽기 트랜잭션이 필요 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

	private static final double SEARCH_RADIUS_KM = 5.0;

	/**
	 * 벡터 검색에 넣을 가게 수 상한. ORA-01795(IN 리스트 1000개 초과) 회피가 유일한 목적이다.
	 *
	 * 품질을 위해 줄이면 안 된다. 30 으로 잡았을 때 실제로 이런 일이 있었다.
	 *   반경 5km 안 80곳 중 30등이 0.60km, 38등이 1.29km 라 사실상 0.6km 컷이 되었고,
	 *   유일한 떡볶이집(동대문엽기떡볶이, 1.29km, 38등)이 후보에 들지 못해
	 *   "떡볶이" 검색에 짬뽕과 토스트가 올라왔다.
	 *
	 * 자르는 기준은 거리인데 고르는 기준은 의미다. 두 축이 다르므로 거리로 먼저
	 * 좁히면 5km 안에 있는 정답을 놓친다. 진짜 제한은 반경 필터가 이미 하고 있다.
	 */
	private static final int VECTOR_STORE_LIMIT = 300;

	/** 요리 하나당 벡터 검색이 가져올 메뉴 행 수. 여러 가게에 흩어지므로 넉넉히 둔다. */
	private static final int VECTOR_LIMIT = 60;

	/** 요리 하나당 화면에 보여줄 후보 가게 수. */
	private static final int MAX_CANDIDATES = 5;

	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;
	private final MenuRepository menuRepository;
	private final OptionMatcher optionMatcher;
	private final KakaoEtaClient kakaoEtaClient;

	public AnalysisResponse analyze(Long userId, AnalysisRequest request) {

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// ── ① 반경 5km. 거리순 정렬된 상태로 돌아온다 ──────────
		List<Restaurant> nearby = findNearby(user, request.preferences());
		if (nearby.isEmpty()) {
			return new AnalysisResponse(List.of(), List.of());
		}

		// ── ② Path A : 브랜드가 맞는 지점 ─────────────────────
		List<ExactMatch> exactMatches = findExactMatches(user, request, nearby);

		// ── ③ Path B : 요리별 후보 가게 ───────────────────────
		List<DishResult> dishResults = findDishResults(user, request, nearby, exactMatches);

		// ── ④ 응답에 실린 가게만 실제 이동시간으로 바꾼다 ─────
		return applyEta(user, nearby, exactMatches, dishResults);
	}

	// ────────────────────────────────────────────────────────
	//  ① 반경
	// ────────────────────────────────────────────────────────
	private List<Restaurant> findNearby(User user, AnalysisRequest.Preferences prefs) {
		double lat = user.getLat();
		double lng = user.getLng();
		double dLat = GeoSupport.latDelta(SEARCH_RADIUS_KM);
		double dLng = GeoSupport.lngDelta(SEARCH_RADIUS_KM, lat);

		// 박스로 먼저 거른다(인덱스). 원은 자바에서 다듬는다.
		List<Restaurant> box = restaurantRepository.findInBox(
			lat - dLat, lat + dLat, lng - dLng, lng + dLng);

		Integer maxDeliveryMin = prefs == null ? null : prefs.maxDeliveryMin();

		return box.stream()
			.filter(r -> distanceTo(user, r) <= SEARCH_RADIUS_KM)
			.filter(r -> maxDeliveryMin == null
				|| (r.getDeliveryMin() != null && r.getDeliveryMin() <= maxDeliveryMin))
			.sorted(Comparator.comparingDouble(r -> distanceTo(user, r)))
			.toList();
	}

	// ────────────────────────────────────────────────────────
	//  ② Path A : brandName 매칭
	// ────────────────────────────────────────────────────────
	private List<ExactMatch> findExactMatches(User user, AnalysisRequest request, List<Restaurant> nearby) {

		// 영상에 나온 브랜드별로 dish 를 모은다. 한 브랜드에서 두 메뉴를 먹었으면 한 카드에 담긴다.
		Map<String, List<AnalysisRequest.Dish>> byBrand = request.extracted().dishes().stream()
			.filter(d -> d.brandName() != null && !d.brandName().isBlank())
			.collect(Collectors.groupingBy(AnalysisRequest.Dish::brandName,
				LinkedHashMap::new, Collectors.toList()));

		List<ExactMatch> result = new ArrayList<>();

		for (Map.Entry<String, List<AnalysisRequest.Dish>> entry : byBrand.entrySet()) {
			Restaurant store = findStoreByBrand(nearby, entry.getKey());
			if (store == null) {
				log.debug("브랜드 '{}' 가 반경 안에 없다. dishResults 가 대신 채운다.", entry.getKey());
				continue;
			}

			List<Menu> storeMenus = menuRepository.findAllByRestaurantId(store.getId());
			List<ItemResponse> items = new ArrayList<>();

			for (AnalysisRequest.Dish dish : entry.getValue()) {
				findMenuByName(storeMenus, dish.name())
					.filter(m -> isOrderable(m, request.preferences()))
					.map(m -> toItem(m, dish.options(), request).item())
					.ifPresent(items::add);
			}

			if (!items.isEmpty()) {
				result.add(new ExactMatch(entry.getKey(), toStore(user, store), items, sumLineTotal(items)));
			}
		}
		return result;
	}

	/**
	 * 브랜드 매칭. 정확 일치 → 부분 일치(긴 브랜드명부터) 순으로 본다.
	 *
	 * 완전 일치만 보면 거의 안 맞는다. 영상 자막은 "엽기떡볶이" 라고 부르는데 DB 에는
	 * "동대문엽기떡볶이" 같은 정식 상호가 들어가기 때문이다. 프론트가 DB 브랜드명을
	 * 알 방법도 없으므로, 이 어긋남은 예외가 아니라 정상 상황이다.
	 *
	 * 그래도 정확 일치를 먼저 소진한다. "본죽" 과 "본죽&비빔밥" 이 둘 다 있을 때
	 * 부분 일치부터 보면 "본죽" 요청에 "본죽&비빔밥" 이 걸린다.
	 *
	 * nearby 가 거리순이라 어느 단계든 첫 번째가 그 브랜드의 최근접 지점이다.
	 *
	 * 줄임말("엽떡")은 여전히 못 잡는다. 그건 restaurant 에 브랜드 별칭 컬럼이 필요하다.
	 */
	private Restaurant findStoreByBrand(List<Restaurant> nearby, String brandName) {
		String wanted = normalize(brandName);
		if (wanted.isEmpty()) {
			return null;
		}

		Optional<Restaurant> exact = nearby.stream()
			.filter(r -> normalize(r.getBrandName()).equals(wanted))
			.findFirst();
		if (exact.isPresent()) {
			return exact.get();
		}

		// 긴 브랜드명부터. 짧은 이름이 아무 데나 걸리는 것을 줄인다.
		return nearby.stream()
			.filter(r -> r.getBrandName() != null && !r.getBrandName().isBlank())
			.sorted(Comparator.comparingInt((Restaurant r) -> r.getBrandName().length()).reversed())
			.filter(r -> {
				String brand = normalize(r.getBrandName());
				return brand.contains(wanted) || wanted.contains(brand);
			})
			.findFirst()
			.orElse(null);
	}

	/**
	 * 메뉴명 매칭. 정확 일치 → 별칭 → 부분 일치 순으로 본다.
	 *
	 * 양방향 contains 를 바로 쓰면 "면", "콜라" 같은 짧은 메뉴명이 거의 모든 dish 에
	 * 걸린다. 확실한 것부터 소진하고 마지막에만 느슨하게 본다.
	 */
	private Optional<Menu> findMenuByName(List<Menu> menus, String dishName) {
		String target = normalize(dishName);
		if (target.isEmpty()) {
			return Optional.empty();
		}

		Optional<Menu> exact = menus.stream()
			.filter(m -> normalize(m.getName()).equals(target))
			.findFirst();
		if (exact.isPresent()) {
			return exact;
		}

		Optional<Menu> byAlias = menus.stream()
			.filter(m -> aliasList(m).stream().anyMatch(a -> a.equals(target)))
			.findFirst();
		if (byAlias.isPresent()) {
			return byAlias;
		}

		// 부분 일치는 긴 메뉴명부터. "짜장면" 이 "면" 보다 먼저 기회를 갖는다.
		return menus.stream()
			.filter(m -> m.getName() != null && !m.getName().isBlank())
			.sorted(Comparator.comparingInt((Menu m) -> m.getName().length()).reversed())
			.filter(m -> {
				String name = normalize(m.getName());
				return name.contains(target) || target.contains(name);
			})
			.findFirst();
	}

	private List<String> aliasList(Menu menu) {
		if (menu.getAliases() == null) {
			return List.of();
		}
		return java.util.Arrays.stream(menu.getAliases().split(","))
			.map(AnalysisService::normalize)
			.filter(s -> !s.isEmpty())
			.toList();
	}

	// ────────────────────────────────────────────────────────
	//  ③ Path B : 요리별 벡터 검색
	// ────────────────────────────────────────────────────────
	private List<DishResult> findDishResults(User user, AnalysisRequest request,
		List<Restaurant> nearby, List<ExactMatch> exactMatches) {

		// 위 카드에 이미 실린 지점은 후보에서 뺀다. 브랜드가 아니라 지점 단위다.
		// 브랜드로 빼면 우선순위에 밀린 다른 지점까지 사라지고, 카드를 못 만든
		// 브랜드의 가게가 화면 어디에도 안 나오게 된다.
		Set<Long> exactStoreIds = exactMatches.stream()
			.map(m -> m.restaurant().restaurantId())
			.collect(Collectors.toSet());

		// nearby 가 거리순이므로 브랜드당 먼저 만난 것이 가장 가까운 지점이다.
		// 접지 않으면 같은 브랜드 지점들이 후보 30칸을 나눠 먹어 다양성이 죽는다.
		List<Restaurant> candidates = nearby.stream()
			.filter(r -> !exactStoreIds.contains(r.getId()))
			.collect(Collectors.toMap(
				AnalysisService::brandKey,
				r -> r,
				(nearer, farther) -> nearer,
				LinkedHashMap::new))
			.values().stream()
			.limit(VECTOR_STORE_LIMIT)
			.toList();

		if (candidates.isEmpty()) {
			return List.of();
		}

		Map<Long, Restaurant> byId = candidates.stream()
			.collect(Collectors.toMap(Restaurant::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
		List<Long> storeIds = List.copyOf(byId.keySet());

		Integer maxSpiceRank = maxSpiceRank(request.preferences());
		boolean excludeMeat = request.preferences() != null && request.preferences().isExcludeMeat();

		List<DishResult> results = new ArrayList<>();

		for (AnalysisRequest.Dish dish : request.extracted().dishes()) {
			results.add(searchOneDish(user, request, dish, storeIds, byId, maxSpiceRank, excludeMeat));
		}
		return results;
	}

	/** 요리 하나에 대해 후보 가게를 찾는다. 임베딩 API 호출은 여기서 정확히 1회다. */
	private DishResult searchOneDish(User user, AnalysisRequest request, AnalysisRequest.Dish dish,
		List<Long> storeIds, Map<Long, Restaurant> byId, Integer maxSpiceRank, boolean excludeMeat) {

		List<MenuMatch> matches = menuRepository.searchSimilar(
			buildQueryText(dish), storeIds, maxSpiceRank, excludeMeat, VECTOR_LIMIT);
		if (matches.isEmpty()) {
			return new DishResult(dish.name(), List.of());
		}

		Map<Long, Menu> menuById = menuRepository
			.findAllByIdIn(matches.stream().map(MenuMatch::menuId).toList()).stream()
			.collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a));

		// 가게당 하나만 남긴다. matches 가 유사도순이라 먼저 만난 것이 그 가게의 최선이다.
		Map<Long, Candidate> bestPerStore = new LinkedHashMap<>();

		for (MenuMatch match : matches) {
			Menu menu = menuById.get(match.menuId());
			if (menu == null) {
				continue;
			}
			Restaurant store = byId.get(menu.getRestaurantId());
			if (store == null || bestPerStore.containsKey(store.getId())) {
				continue;
			}
			// 이 요리의 옵션만 넘긴다. 다른 요리 옵션을 섞으면 토스트에 떡볶이용
			// "치즈 추가" 가 켜져 결제액이 조용히 올라간다.
			ScoredItem scored = toItem(menu, dish.options(), request);
			bestPerStore.put(store.getId(), new Candidate(
				toStore(user, store), scored.item(),
				score(match.similarity(), scored.optionMatchRatio())));
		}

		List<Candidate> top = bestPerStore.values().stream()
			.sorted(Comparator.comparingDouble(Candidate::score).reversed())
			.limit(MAX_CANDIDATES)
			.toList();

		return new DishResult(dish.name(), top);
	}

	/**
	 * 임베딩할 질의문. 요리 하나만 담는다.
	 *
	 * options 를 넣지 않는다. 옵션은 "무엇을 먹을지" 가 아니라 "그 메뉴를 어떻게 바꿀지"
	 * 라서 메뉴의 정체성이 아니다. 질의에 섞으면 옵션 이름이 메뉴 이름과 같은 무게로
	 * 경쟁한다. 실제로 "떡볶이 … 치즈 추가" 로 검색했더니 갈릭베이컨치즈와 햄치즈토스트가
	 * 1·2 등으로 올라왔다. 옵션은 OptionMatcher 가 따로 매칭하므로 이중 계산이기도 하다.
	 *
	 * keywords 도 넣지 않는다. 영상 전체에 붙는 값이라 모든 요리 질의에 같은 잡음이
	 * 섞인다. "바삭한" 이 떡볶이 질의에 들어가면 튀김류가 끌려 올라온다.
	 * keywords 는 decideSpice 에서만 쓴다.
	 *
	 * 브랜드와 카테고리도 넣지 않는다. 브랜드는 Path A 가 담당하고, 카테고리를 섞으면
	 * 그 카테고리 메뉴 쪽으로 벡터가 쏠린다.
	 */
	private String buildQueryText(AnalysisRequest.Dish dish) {
		StringBuilder sb = new StringBuilder(dish.name());
		if (dish.description() != null) {
			sb.append(' ').append(dish.description());
		}
		return sb.toString().trim();
	}

	/**
	 * 코사인 거리는 [0, 2] 라 1 - distance 는 [-1, 1] 이 된다.
	 * 정렬에는 문제가 없지만 음수 점수가 클라이언트에 나가면 이상하므로 0 으로 자른다.
	 */
	private double score(double similarity, double optionMatchRatio) {
		double raw = Math.max(0.0, similarity) * 0.9 + optionMatchRatio * 0.1;
		return Math.round(raw * 100) / 100.0;
	}

	/** 프랜차이즈가 아니면 지점 중복이 없으므로 각자 다른 가게로 센다. */
	private static String brandKey(Restaurant restaurant) {
		String brand = restaurant.getBrandName();
		return (brand == null || brand.isBlank()) ? "#" + restaurant.getId() : brand;
	}

	// ────────────────────────────────────────────────────────
	//  조립
	// ────────────────────────────────────────────────────────
	/**
	 * 조립 결과. optionMatchRatio 는 점수 계산에만 쓰고 응답에는 나가지 않는다.
	 *
	 * 응답의 options 에는 켜진 것만 담기므로, 매칭률을 나중에 items 로부터 되계산할 수
	 * 없다(분모가 사라진다). 그래서 여기서 계산해 함께 들고 나온다.
	 */
	private record ScoredItem(ItemResponse item, double optionMatchRatio) {
	}

	private ScoredItem toItem(Menu menu, List<String> videoPhrases, AnalysisRequest request) {

		List<MenuOption> all = optionMatcher.parse(menu.getOptions());
		Set<String> picked = optionMatcher.pickMentioned(all, videoPhrases);

		// 켜진 옵션만 내보낸다. 안 고른 선택지까지 실으면 응답이 몇 배로 커진다.
		List<OptionResponse> options = all.stream()
			.filter(o -> picked.contains(o.name()))
			.map(o -> new OptionResponse(
				o.group(), o.name(), o.price() == null ? 0 : o.price(), true))
			.toList();

		int optionsPrice = options.stream().mapToInt(OptionResponse::price).sum();
		int quantity = 1;

		ItemResponse item = new ItemResponse(
			menu.getId(), menu.getName(), menu.getMenuType(), menu.getPrice(), menu.getImageUrl(),
			quantity,
			menu.getSpiceLevel(), Boolean.TRUE.equals(menu.getSpiceAdjustable()),
			decideSpice(menu, request),
			options, optionsPrice,
			(menu.getPrice() + optionsPrice) * quantity);

		double ratio = all.isEmpty() ? 0.0 : (double)options.size() / all.size();
		return new ScoredItem(item, ratio);
	}

	/**
	 * 맵기는 내리기만 하고 올리지 않는다.
	 * 사용자가 HOT 까지 먹을 수 있다고 해서 순한 메뉴를 맵게 바꾸면
	 * 시키지도 않은 것을 맵게 만들어 주는 셈이다.
	 */
	private SpiceLevel decideSpice(Menu menu, AnalysisRequest request) {
		if (!Boolean.TRUE.equals(menu.getSpiceAdjustable())) {
			return null;   // 조절 불가. 프론트가 버튼을 안 그린다.
		}
		List<String> keywords = request.extracted().keywords();
		boolean spicyInVideo = keywords != null && keywords.stream()
			.anyMatch(k -> k.contains("매운") || k.contains("매콤") || k.contains("불닭") || k.contains("얼큰"));

		SpiceLevel fromVideo = spicyInVideo ? SpiceLevel.HOT : SpiceLevel.MEDIUM;
		SpiceLevel max = request.preferences() == null ? null : request.preferences().maxSpiceLevel();

		return fromVideo.isWithin(max) ? fromVideo : max;
	}

	/**
	 * etaMin 을 가게 고정값에서 실제 이동시간으로 바꾼다.
	 *
	 * 응답에 실린 가게만 대상으로 한 번 호출한다. 반경 안 79곳을 전부 물어보면
	 * 쓰지도 않을 가게에 왕복을 쓰게 된다.
	 *
	 * 길찾기가 실패하거나 특정 가게의 길을 못 찾으면 그 가게는 delivery_min 을 그대로
	 * 유지한다. 부가 정보 하나 때문에 검색 결과 전체를 버릴 이유가 없다.
	 */
	private AnalysisResponse applyEta(User user, List<Restaurant> nearby,
		List<ExactMatch> exactMatches, List<DishResult> dishResults) {

		Set<Long> ids = new LinkedHashSet<>();
		exactMatches.forEach(m -> ids.add(m.restaurant().restaurantId()));
		dishResults.forEach(d -> d.candidates().forEach(c -> ids.add(c.restaurant().restaurantId())));
		if (ids.isEmpty()) {
			return new AnalysisResponse(exactMatches, dishResults);
		}

		Map<Long, Restaurant> byId = nearby.stream()
			.collect(Collectors.toMap(Restaurant::getId, r -> r, (a, b) -> a));

		List<Restaurant> targets = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
		Map<Long, Integer> travel =
			kakaoEtaClient.travelMinutes(user.getLat(), user.getLng(), targets);
		if (travel.isEmpty()) {
			return new AnalysisResponse(exactMatches, dishResults);
		}

		return new AnalysisResponse(
			exactMatches.stream()
				.map(m -> new ExactMatch(m.brandName(), withEta(m.restaurant(), byId, travel),
					m.items(), m.totalPrice()))
				.toList(),
			dishResults.stream()
				.map(d -> new DishResult(d.dishName(), d.candidates().stream()
					.map(c -> new Candidate(withEta(c.restaurant(), byId, travel), c.item(), c.score()))
					.toList()))
				.toList());
	}

	/** 도착 예정 = 가게 조리시간 + 이동시간. 이동시간을 못 구했으면 원래 값을 둔다. */
	private StoreResponse withEta(StoreResponse store, Map<Long, Restaurant> byId,
		Map<Long, Integer> travel) {

		Integer minutes = travel.get(store.restaurantId());
		if (minutes == null) {
			return store;
		}
		Restaurant entity = byId.get(store.restaurantId());
		int prep = (entity != null && entity.getPrepMin() != null) ? entity.getPrepMin() : 0;

		return new StoreResponse(
			store.restaurantId(), store.name(), store.foodCategory(), store.area(), store.rating(),
			prep + minutes,
			store.deliveryFee(), store.minOrderPrice(), store.distanceKm(), store.imageUrl());
	}

	private StoreResponse toStore(User user, Restaurant store) {
		return new StoreResponse(
			store.getId(), store.getName(), store.getFoodCategory(), store.getArea(), store.getRating(),
			store.getDeliveryMin(),          // applyEta 가 실제 이동시간으로 덮어쓴다
			store.getDeliveryFee(), store.getMinOrderPrice(),
			Math.round(distanceTo(user, store) * 100) / 100.0,
			store.getImageUrl());
	}

	// ────────────────────────────────────────────────────────
	//  잡다
	// ────────────────────────────────────────────────────────
	private double distanceTo(User user, Restaurant store) {
		return GeoSupport.distanceKm(user.getLat(), user.getLng(), store.getLat(), store.getLng());
	}

	private boolean isOrderable(Menu menu, AnalysisRequest.Preferences prefs) {
		if (prefs == null) {
			return true;
		}
		if (prefs.isExcludeMeat() && Boolean.TRUE.equals(menu.getHasMeat())) {
			return false;
		}
		// 스키마상 NOT NULL 이지만, null 이면 NPE 로 요청 전체가 죽는다.
		// 맵기를 모르는 메뉴는 조건이 걸려 있을 때 통과시키지 않는다(SQL 쪽과 같은 판단).
		SpiceLevel level = menu.getSpiceLevel();
		return level == null ? prefs.maxSpiceLevel() == null : level.isWithin(prefs.maxSpiceLevel());
	}

	private Integer maxSpiceRank(AnalysisRequest.Preferences prefs) {
		if (prefs == null || prefs.maxSpiceLevel() == null) {
			return null;
		}
		return prefs.maxSpiceLevel().getRank();
	}

	private int sumLineTotal(List<ItemResponse> items) {
		return items.stream().mapToInt(ItemResponse::lineTotal).sum();
	}

	private static String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
	}
}
