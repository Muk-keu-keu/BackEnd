package mukkeu.mukkeu.order.dto;

import java.util.List;

/**
 * 201 응답. 가게 이름만 돌려준다.
 *
 * 요청 내용을 되돌려주지 않는다. 프론트가 방금 보낸 값이라 쓸 데가 없다.
 * 건수도 따로 주지 않는다. restaurantNames.size() 가 곧 건수다.
 * checkoutId 도 주지 않는다. 완료 화면에서 "주문 내역 보기" 로 목록에 가는 흐름이면
 * 필요가 없고, 상세로 바로 보내는 버튼을 달게 되면 그때 붙인다.
 */
public record OrderCreateResponse(
	List<String> restaurantNames,

	/**
	 * 결제 전체의 포인트 순변화. 음수면 포인트를 쓴 것, 양수면 쌓인 것이다.
	 * 완료 화면은 이 값 하나만 쓴다 — "포인트 5,000원 사용" / "7,000P 남았어요".
	 */
	Integer pointDelta,

	/** 실제로 결제된 현금 합. 잔액이 넉넉하면 0 이 될 수 있다. */
	Integer paidCash,

	/** 가게별 결과. 잔액을 화면에 바로 반영하려면 필요하다. */
	List<StorePoint> points
) {

	public record StorePoint(
		Long restaurantId,
		String restaurantName,

		/** 화면에 그대로 쓰지 않는다. 정산·디버깅용 상세다. */
		Integer usedPoint,
		Integer earnedPoint,

		/** 이 결제 뒤 남은 잔액. */
		Integer balance
	) {
	}
}
