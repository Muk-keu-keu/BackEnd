package mukkeu.mukkeu.menu.dto;

import java.util.List;

import mukkeu.mukkeu.menu.domain.MenuType;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.restaurant.dto.RestaurantSummary;

/**
 * 가게 하나의 메뉴판 전체.
 *
 * 먹방요기 응답은 "영상에서 언급된" 옵션만 켜서 보낸다. 사용자가 그걸 고치려면
 * 고르지 않은 선택지까지 필요한데, 그 목록을 주는 곳이 여기다.
 *
 * restaurant 는 먹방요기 결과와 똑같은 RestaurantSummary 다. 예전에는 id 와 이름만
 * 줬는데, 메뉴 수정 화면에도 가게 헤더(평점·배달비·최소주문·거리·도착예정)가 그대로
 * 붙는다. 프론트가 앞 화면 값을 들고 다니게 하면 새로고침 한 번에 사라지고,
 * 이 API 를 딥링크로 바로 열 수도 없다.
 *
 * creditBalance 는 RestaurantSummary 가 아니라 여기 최상위에 둔다. RestaurantSummary 는
 * 먹방요기 응답에도 실려서, 거기에 넣으면 분석 API 의 응답 모양까지 바뀐다. 잔액은
 * 메뉴판을 여는 이 화면에서만 필요하다.
 *
 * 맵기·고기 같은 preferences 필터를 걸지 않는다. 사용자가 직접 메뉴판을 열어
 * 고르는 화면이므로, 서버가 선택지를 미리 지우면 "왜 이 메뉴가 없지" 가 된다.
 */
public record MenuBoardResponse(
	RestaurantSummary restaurant,

	/**
	 * 이 가게에 남은 채움 포인트. 비로그인이면 null, 거래가 없었으면 0 이다.
	 * 화면은 0 이하면 배지를 통째로 숨긴다.
	 */
	Integer creditBalance,

	List<MenuItem> menus
) {

	public record MenuItem(
		Long menuId,
		String name,
		MenuType menuType,
		String description,
		int price,
		String imageUrl,
		SpiceLevel spiceLevel,
		boolean spiceAdjustable,
		List<OptionItem> options      // 그 메뉴가 가진 옵션 전부. 없으면 빈 배열
	) {
	}

	/** selected 가 없다. 메뉴판에는 고른다는 개념이 없다. */
	public record OptionItem(
		String group,
		String name,
		int price
	) {
	}
}
