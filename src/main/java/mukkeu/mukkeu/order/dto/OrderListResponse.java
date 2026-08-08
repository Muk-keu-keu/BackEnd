package mukkeu.mukkeu.order.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 결제 내역 목록. 카드 하나 = 결제 하나 = 영상 하나.
 *
 * items 를 담지 않는다. 카드에는 가게 이름과 총액만 나가므로 order_item 을 읽지 않는다.
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
		int totalPrice                   // 그 결제의 모든 가게 합
	) {
	}
}
