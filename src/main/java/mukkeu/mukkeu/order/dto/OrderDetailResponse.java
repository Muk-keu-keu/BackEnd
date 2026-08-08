package mukkeu.mukkeu.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.SpiceLevel;

/**
 * 결제 내역 상세. stores 구조가 결제 요청과 같은 모양이라 프론트가 변환할 것이 없다.
 *
 * 금액과 이름은 전부 결제 시점 스냅샷이다. 메뉴 가격이 오르거나 메뉴가 사라져도
 * 지난 내역은 그대로 남는다.
 */
public record OrderDetailResponse(
	Long checkoutId,
	OffsetDateTime orderedAt,
	SourceResponse source,
	List<Store> stores,
	int totalPrice                       // stores 의 subtotal 합
) {

	public record Store(
		Long restaurantId,
		String restaurantName,
		int deliveryFee,
		List<Item> items,
		int itemsTotal,
		int subtotal                     // itemsTotal + deliveryFee
	) {
	}

	public record Item(
		Long menuId,
		String menuName,
		String menuImageUrl,             // 결제 시점 스냅샷. 화면에 썸네일을 그린다
		int unitPrice,
		int quantity,
		SpiceLevel selectedSpice,
		List<MenuOption> selectedOptions,
		int optionsPrice,
		int lineTotal
	) {
	}
}
