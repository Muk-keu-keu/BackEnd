package mukkeu.mukkeu.menu.app;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import mukkeu.mukkeu.credit.app.CreditService;
import mukkeu.mukkeu.global.client.KakaoEtaClient;
import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.global.support.GeoSupport;
import mukkeu.mukkeu.menu.domain.Menu;
import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.repository.MenuRepository;
import mukkeu.mukkeu.menu.dto.MenuBoardResponse;
import mukkeu.mukkeu.restaurant.app.RestaurantSummaryFactory;
import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.domain.repository.RestaurantRepository;
import mukkeu.mukkeu.restaurant.dto.RestaurantSummary;
import mukkeu.mukkeu.user.domain.User;
import mukkeu.mukkeu.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 가게 메뉴판 조회.
 *
 * 프론트가 먹방요기 결과에서 "메뉴 수정" 을 눌렀을 때 쓴다. 먹방요기 응답에는
 * 켜진 옵션만 담기므로, 나머지 선택지는 여기서 받아야 한다.
 *
 * 가게 요약(restaurant)도 같이 내려준다. 거리와 도착예정은 로그인한 사용자 좌표를
 * 기준으로 이 요청에서 다시 계산한다. 먹방요기와 값이 어긋나면 사용자는 같은 가게가
 * 화면마다 다른 시간을 말한다고 느낀다.
 */
@Service
@RequiredArgsConstructor
public class MenuBoardService {

	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;
	private final MenuRepository menuRepository;
	private final OptionMatcher optionMatcher;
	private final KakaoEtaClient kakaoEtaClient;
	private final CreditService creditService;

	public MenuBoardResponse getBoard(Long userId, Long restaurantId) {

		Restaurant store = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

		// MAIN 을 먼저, 같은 종류면 메뉴 번호 순. 메뉴판 순서가 요청마다 바뀌면 안 된다.
		List<MenuBoardResponse.MenuItem> menus = menuRepository.findAllByRestaurantId(restaurantId).stream()
			.sorted(Comparator.<Menu>comparingInt(MenuBoardService::typeOrder)
				.thenComparing(Menu::getId))
			.map(this::toItem)
			.toList();

		// 비로그인이면 null. 0 으로 채우지 않는다 — "포인트가 없다" 와 "누구인지 모른다" 는 다르다.
		Integer creditBalance = userId == null ? null : creditService.balanceOf(userId, restaurantId);

		return new MenuBoardResponse(toSummary(userId, store), creditBalance, menus);
	}

	/**
	 * 거리·도착예정을 사용자 기준으로 채운다.
	 *
	 * 사용자를 못 찾거나 좌표가 없으면 거리는 null, 도착예정은 가게가 적어 둔
	 * delivery_min 이 나간다. 메뉴판은 좌표가 없어도 볼 수 있어야 하므로
	 * 여기서 예외를 던지지 않는다.
	 */
	private RestaurantSummary toSummary(Long userId, Restaurant store) {

		User user = userId == null ? null : userRepository.findById(userId).orElse(null);
		if (user == null || user.getLat() == null || user.getLng() == null) {
			return RestaurantSummaryFactory.of(store, null, store.getDeliveryMin());
		}

		Double distanceKm = GeoSupport.distanceKm(
			user.getLat(), user.getLng(), store.getLat(), store.getLng());

		// 목적지가 하나뿐이라 왕복도 한 번이다. 실패하면 빈 맵이 오고 delivery_min 으로 돌아간다.
		Map<Long, Integer> travel =
			kakaoEtaClient.travelMinutes(user.getLat(), user.getLng(), List.of(store));

		Integer minutes = travel.get(store.getId());
		Integer etaMin = store.getDeliveryMin();
		if (minutes != null) {
			etaMin = (store.getPrepMin() == null ? 0 : store.getPrepMin()) + minutes;
		}

		return RestaurantSummaryFactory.of(store, distanceKm, etaMin);
	}

	private MenuBoardResponse.MenuItem toItem(Menu menu) {
		List<MenuOption> options = optionMatcher.parse(menu.getOptions());

		return new MenuBoardResponse.MenuItem(
			menu.getId(), menu.getName(), menu.getMenuType(), menu.getDescription(),
			menu.getPrice(), menu.getImageUrl(),
			menu.getSpiceLevel(), Boolean.TRUE.equals(menu.getSpiceAdjustable()),
			options.stream()
				.map(o -> new MenuBoardResponse.OptionItem(
					o.group(), o.name(), o.price() == null ? 0 : o.price()))
				.toList());
	}

	private static int typeOrder(Menu menu) {
		if (menu.getMenuType() == null) {
			return 99;
		}
		return switch (menu.getMenuType()) {
			case MAIN -> 0;
			case SIDE -> 1;
			case DRINK -> 2;
		};
	}
}
