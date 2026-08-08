package mukkeu.mukkeu.menu.app;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.menu.domain.Menu;
import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.repository.MenuRepository;
import mukkeu.mukkeu.menu.dto.MenuBoardResponse;
import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.domain.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;

/**
 * 가게 메뉴판 조회.
 *
 * 프론트가 먹방요기 결과에서 "메뉴 수정" 을 눌렀을 때 쓴다. 먹방요기 응답에는
 * 켜진 옵션만 담기므로, 나머지 선택지는 여기서 받아야 한다.
 */
@Service
@RequiredArgsConstructor
public class MenuBoardService {

	private final RestaurantRepository restaurantRepository;
	private final MenuRepository menuRepository;
	private final OptionMatcher optionMatcher;

	public MenuBoardResponse getBoard(Long restaurantId) {

		Restaurant store = restaurantRepository.findById(restaurantId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

		// MAIN 을 먼저, 같은 종류면 메뉴 번호 순. 메뉴판 순서가 요청마다 바뀌면 안 된다.
		List<MenuBoardResponse.MenuItem> menus = menuRepository.findAllByRestaurantId(restaurantId).stream()
			.sorted(Comparator.<Menu>comparingInt(MenuBoardService::typeOrder)
				.thenComparing(Menu::getId))
			.map(this::toItem)
			.toList();

		return new MenuBoardResponse(store.getId(), store.getName(), menus);
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
