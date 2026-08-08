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
	List<String> restaurantNames
) {
}
