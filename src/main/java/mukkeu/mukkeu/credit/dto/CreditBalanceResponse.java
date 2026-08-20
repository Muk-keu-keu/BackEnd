package mukkeu.mukkeu.credit.dto;

import java.util.List;

/**
 * 내 채움 포인트 잔액.
 *
 * 포인트가 가게 전용이라 "총 잔액" 이라는 값이 의미가 약하다. 홍콩반점 7,000P 와
 * 교촌 3,000P 를 더한 10,000P 로는 아무것도 할 수 없다. 그래서 합계를 먼저 보여주지
 * 않고 가게별 목록을 그대로 내려준다. 화면이 합계를 쓰고 싶으면 더하면 된다.
 *
 * 잔액 0 인 가게는 담지 않는다. 지난 거래 기록이 아니라 "지금 쓸 수 있는 것" 을 보는
 * 화면이기 때문이다.
 */
public record CreditBalanceResponse(
	List<StoreCredit> credits
) {

	public record StoreCredit(
		Long restaurantId,
		String restaurantName,
		Integer balance,
		String imageUrl
	) {
	}
}
