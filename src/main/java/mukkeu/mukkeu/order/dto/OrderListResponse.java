package mukkeu.mukkeu.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 결제 내역 목록. 카드 하나 = 결제 하나 = 영상 하나.
 *
 * 옵션·단가 같은 상세는 담지 않는다. 그건 상세에서 받는다.
 * 다만 메뉴 **이름**은 담는다. 카드가 "[지점명] 메뉴, 메뉴" 한 줄을 그리기 때문이다
 * (시안 857:4509). 가게 이름만으로는 무엇을 시켰는지 알 수 없어 카드를 열어봐야 한다.
 * 이름만 읽는 것이라 order_item 조회가 페이지당 쿼리 한 번 늘 뿐 N+1 이 아니다.
 *
 * nextCursor 는 이 페이지 마지막 카드의 checkoutId 다. 다음 요청에 그대로 넣으면 된다.
 * 더 없으면 null 이다.
 */
public record OrderListResponse(
	List<Card> orders,
	String nextCursor
) {

	public record Card(
		Long checkoutId,
		OffsetDateTime orderedAt,        // +09:00 이 붙는다. 프론트가 시각을 확정할 수 있다
		SourceResponse source,
		List<String> restaurantNames,
		int totalPrice,                  // 그 결제의 모든 가게 합
		List<StoreMenus> menuSummary     // 가게별 메뉴 이름. restaurantNames 와 같은 순서다
	) {
	}

	/** 카드의 "[지점명] 메뉴, 메뉴" 한 줄. */
	public record StoreMenus(
		String storeName,
		List<String> menuNames
	) {
	}
}
