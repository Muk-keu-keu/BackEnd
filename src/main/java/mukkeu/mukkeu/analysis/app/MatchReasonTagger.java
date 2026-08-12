package mukkeu.mukkeu.analysis.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import mukkeu.mukkeu.analysis.domain.MatchReasonTag;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.Candidate;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.DishResult;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.ExactMatch;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.ItemResponse;
import mukkeu.mukkeu.analysis.dto.AnalysisResponse.OptionResponse;
import mukkeu.mukkeu.restaurant.dto.RestaurantSummary;

/**
 * 카드마다 "왜 이걸 골랐나" 태그와 한 줄 문구를 붙인다.
 *
 * 응답 조립이 전부 끝난 뒤 마지막에 한 번 훑는 장식(decoration) 단계다.
 * 검색 단계(searchOneDish)에서 붙이지 않는 이유가 둘 있다.
 *   거리·ETA 가 applyEta 이후에야 확정되고,
 *   요리별 1등·최근접은 그 요리 후보 전체가 모여야 계산할 수 있다.
 * 검색 로직에 표현 관심사가 섞이지 않는 것은 덤이다.
 *
 * ── 태그를 아예 안 붙이는 카드가 생긴다. 의도된 것이다 ──
 * 모든 카드에 배지를 달면 배지가 정보가 아니라 장식이 된다. 특히 "조건에 맞아요" 류는
 * 응답에 실린 모든 후보에 참이라 붙이는 순간 의미가 사라진다.
 * 내세울 것이 있는 카드에만 붙여야 나머지와 대비되어 눈에 들어온다.
 * 태그가 없어도 카드에는 가게명·메뉴·가격·거리·ETA 가 다 있어 비어 보이지 않는다.
 *
 * ── DISTANCE 를 순위와 절대 기준 둘 다로 판정하는 이유 ──
 * 거리·ETA 는 카드에 숫자로 이미 나가 있다. 그래서 "0.45km, 24분이면 와요" 같은 문구는
 * 카드를 되읽어 줄 뿐 정보량이 0 이다. 대신 후보들 사이의 순위를 말한다 — 그건 카드가
 * 보여줄 수 없는 사실이고, 요리당 한 장뿐이라 희소해서 눈에 들어온다.
 */
@Component
public class MatchReasonTagger {

	/** 이 안쪽이어야 "가깝다" 고 말한다. */
	private static final double NEAR_KM = 1.5;

	/**
	 * 1등 점수와 이만큼 안쪽인 후보만 "이 요리의 후보" 로 인정한다.
	 *
	 * 이 게이트가 없으면 거리만으로 배지가 붙어, 떡볶이 검색에 올라온 마라탕집이
	 * 가깝다는 이유로 추천처럼 보인다. 실제로 그런 응답이 나왔다.
	 * 유사도가 처지는 후보는 애초에 거리를 자랑할 자격이 없다.
	 */
	private static final double RELEVANCE_GAP = 0.05;

	/** 한 카드에 실을 태그 수. 넘치면 문구가 길어져 카드가 부푼다. */
	private static final int MAX_TAGS = 2;

	public AnalysisResponse apply(AnalysisResponse response) {
		return new AnalysisResponse(
			response.summary(),
			response.emptyReason(),
			response.exactMatches().stream().map(this::tagExactMatch).toList(),
			response.dishResults().stream().map(this::tagDishResult).toList());
	}

	// ────────────────────────────────────────────────────────
	//  exactMatches — 무조건 EXACT_MATCH. 옵션이 켜졌으면 하나 더.
	// ────────────────────────────────────────────────────────
	private ExactMatch tagExactMatch(ExactMatch match) {

		List<MatchReasonTag> tags = new ArrayList<>();
		tags.add(MatchReasonTag.EXACT_MATCH);
		if (hasPickedOption(match.items())) {
			tags.add(MatchReasonTag.OPTION_MATCH);
		}

		return new ExactMatch(match.brandName(), match.restaurant(), match.items(), match.totalPrice(),
			tags, reasonOf(tags, null, firstItemWithOption(match.items()), match.restaurant()));
	}

	// ────────────────────────────────────────────────────────
	//  dishResults — 요리 안에서 1등·최근접을 먼저 정하고 카드를 돈다.
	// ────────────────────────────────────────────────────────
	private DishResult tagDishResult(DishResult dish) {

		List<Candidate> candidates = dish.candidates();
		if (candidates.isEmpty()) {
			return dish;
		}

		// 후보가 하나뿐이면 "가장 비슷해요" 가 비교 대상 없이 공허하다.
		Long topScoreId = candidates.size() < 2 ? null
			: candidates.stream()
				.max(Comparator.comparingDouble(Candidate::score))
				.map(c -> c.restaurant().restaurantId())
				.orElse(null);

		// 유사도가 1등과 비슷한 후보만 거리 경쟁에 올린다. 그중 딱 한 장에만 붙인다.
		double topScore = candidates.stream().mapToDouble(Candidate::score).max().orElse(0.0);
		Set<Long> nearIds = candidates.stream()
			.filter(c -> c.score() >= topScore - RELEVANCE_GAP)
			.filter(c -> c.restaurant().distanceKm() != null)
			.filter(c -> c.restaurant().distanceKm() <= NEAR_KM)
			.min(Comparator.comparingDouble(c -> c.restaurant().distanceKm()))
			.map(c -> Set.of(c.restaurant().restaurantId()))
			.orElse(Set.of());

		return new DishResult(dish.dishName(), candidates.stream()
			.map(c -> tagCandidate(c, dish.dishName(), topScoreId, nearIds))
			.toList());
	}

	private Candidate tagCandidate(Candidate candidate, String dishName,
		Long topScoreId, Set<Long> nearIds) {

		Long storeId = candidate.restaurant().restaurantId();

		// 선언 순서가 곧 우선순위라 이 순서로 담기만 하면 정렬이 끝난다.
		List<MatchReasonTag> tags = new ArrayList<>();
		if (!candidate.item().options().isEmpty()) {
			tags.add(MatchReasonTag.OPTION_MATCH);
		}
		if (Objects.equals(storeId, topScoreId)) {
			tags.add(MatchReasonTag.TASTE_SIMILAR);
		}
		if (nearIds.contains(storeId)) {
			tags.add(MatchReasonTag.DISTANCE);
		}

		List<MatchReasonTag> picked = tags.size() <= MAX_TAGS ? tags : tags.subList(0, MAX_TAGS);

		return new Candidate(candidate.restaurant(), candidate.item(), candidate.score(),
			List.copyOf(picked), reasonOf(picked, dishName, candidate.item(), candidate.restaurant()));
	}

	// ────────────────────────────────────────────────────────
	//  문구
	// ────────────────────────────────────────────────────────

	/**
	 * 태그가 여러 개여도 문구는 한 줄이다. 줄을 늘리는 대신 한 문장에 합친다.
	 * 카드는 모바일 목록이라 자리가 좁고, 세 줄이 들어가면 가게명·가격보다 설명이 커진다.
	 */
	private String reasonOf(List<MatchReasonTag> tags, String dishName,
		ItemResponse item, RestaurantSummary store) {

		if (tags.isEmpty()) {
			return null;
		}

		MatchReasonTag first = tags.get(0);
		MatchReasonTag second = tags.size() > 1 ? tags.get(1) : null;

		String option = optionLabel(item);
		String dish = dishName == null ? null : dishName + particleWa(dishName);

		return switch (first) {
			case EXACT_MATCH -> second == MatchReasonTag.OPTION_MATCH && option != null
				? "영상에 나온 그 지점이고 " + option + "까지 담았어요"
				: "영상에 나온 그 지점이에요";

			case OPTION_MATCH -> {
				if (option == null) {
					yield null;
				}
				yield switch (second) {
					case TASTE_SIMILAR -> dish == null
						? option + "도 맞고 가장 비슷해요"
						: option + "도 맞고 영상 속 " + dish + " 가장 비슷해요";
					case DISTANCE -> option + "도 맞고 제일 가까워요";
					case null, default -> option + "까지 맞출 수 있어요";
				};
			}

			case TASTE_SIMILAR -> {
				if (second == MatchReasonTag.DISTANCE) {
					yield "가장 비슷하면서 제일 가까워요";
				}
				yield dish == null ? "영상 속 요리와 가장 비슷해요" : "영상 속 " + dish + " 가장 비슷해요";
			}

			// 거리·ETA 는 카드에 숫자로 이미 있다. 그대로 되읽어 주면 정보량이 0 이라
			// 후보들 사이의 순위라는, 카드가 보여줄 수 없는 사실만 말한다.
			case DISTANCE -> "후보 중 제일 가까워요";
		};
	}

	/** 옵션이 여러 개면 다 나열하지 않는다. 카드 폭이 넘친다. */
	private String optionLabel(ItemResponse item) {
		if (item == null || item.options().isEmpty()) {
			return null;
		}
		List<String> names = item.options().stream().map(OptionResponse::name).toList();
		return names.size() == 1 ? names.get(0) : names.get(0) + " 외 " + (names.size() - 1) + "개";
	}

	private boolean hasPickedOption(List<ItemResponse> items) {
		return items.stream().anyMatch(i -> !i.options().isEmpty());
	}

	private ItemResponse firstItemWithOption(List<ItemResponse> items) {
		return items.stream().filter(i -> !i.options().isEmpty()).findFirst().orElse(null);
	}

	/**
	 * 한글 조사. "떡볶이와" / "후라이드치킨과" 처럼 받침 유무로 갈린다.
	 * 항상 "와" 로 두면 문장이 어색해져 AI 가 쓴 티가 아니라 버그처럼 읽힌다.
	 */
	private String particleWa(String word) {
		if (word == null || word.isBlank()) {
			return "와";
		}
		char last = word.charAt(word.length() - 1);
		if (last < 0xAC00 || last > 0xD7A3) {
			return "와";   // 한글이 아니면(영문·숫자) 기본값
		}
		return (last - 0xAC00) % 28 == 0 ? "와" : "과";
	}
}
