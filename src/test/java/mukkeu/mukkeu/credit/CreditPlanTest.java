package mukkeu.mukkeu.credit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import mukkeu.mukkeu.credit.domain.CreditPlan;

/**
 * 채움 포인트 계산 검증. 스프링을 띄우지 않는다 — 순수 계산이라 그럴 이유가 없다.
 *
 * 가장 중요한 검사는 마지막 항등식이다.
 *   낸 현금 = 받은 음식값 + 포인트 순증 + 배달비
 * 이게 깨지면 어딘가에서 돈이 생기거나 사라진다.
 */
class CreditPlanTest {

	private static final long STORE = 1L;
	private static final int FEE = 2_000;

	@Test
	@DisplayName("1회차 — 잔액이 없으면 미달분을 전부 새로 선불한다")
	void firstOrder() {
		CreditPlan plan = CreditPlan.of(STORE, 8_000, 15_000, FEE, 0, true);

		assertThat(plan.usedPoint()).isZero();
		assertThat(plan.earnedPoint()).isEqualTo(7_000);
		assertThat(plan.payAmount()).isEqualTo(17_000);
		assertThat(plan.balanceAfter()).isEqualTo(7_000);
	}

	@Test
	@DisplayName("2회차 — 쌓인 포인트가 음식값을 대신해 현금이 줄어든다")
	void secondOrder() {
		CreditPlan plan = CreditPlan.of(STORE, 9_000, 15_000, FEE, 7_000, true);

		assertThat(plan.usedPoint()).isEqualTo(7_000);
		assertThat(plan.earnedPoint()).isEqualTo(6_000);
		assertThat(plan.payAmount()).isEqualTo(10_000);   // 17,000 → 10,000
		assertThat(plan.balanceAfter()).isEqualTo(6_000);
	}

	@Test
	@DisplayName("최소주문을 넘기면 적립은 없고 포인트만 소진된다")
	void aboveMinimum() {
		CreditPlan plan = CreditPlan.of(STORE, 20_000, 15_000, FEE, 6_000, true);

		assertThat(plan.earnedPoint()).isZero();
		assertThat(plan.usedPoint()).isEqualTo(6_000);
		assertThat(plan.payAmount()).isEqualTo(16_000);
		assertThat(plan.balanceAfter()).isZero();
	}

	@Test
	@DisplayName("잔액이 넉넉하면 배달비까지 포인트로 내고 결제액이 0이 된다")
	void spendsPointsFirst() {
		CreditPlan plan = CreditPlan.of(STORE, 3_000, 15_000, FEE, 20_000, true);

		assertThat(plan.usedPoint()).isEqualTo(17_000);   // 배달비까지 포인트로
		assertThat(plan.payAmount()).isZero();            // ★ 결제할 현금이 없다
		assertThat(plan.earnedPoint()).isEqualTo(12_000); // 미달분은 그대로 돌아온다
		assertThat(plan.balanceAfter()).isEqualTo(15_000);
		assertThat(plan.balanceDelta()).isEqualTo(-5_000); // 음식 3,000 + 배달비 2,000
	}

	@Test
	@DisplayName("잔액이 충분하면 잔액은 음식값 + 배달비만큼만 줄어든다")
	void balanceFallsByFoodAndDelivery() {
		CreditPlan plan = CreditPlan.of(STORE, 12_000, 15_000, FEE, 30_000, true);

		assertThat(plan.balanceDelta()).isEqualTo(-14_000);  // 12,000 + 2,000
		assertThat(plan.payAmount()).isZero();
	}

	@Test
	@DisplayName("usePrepaid=false 면 포인트를 쓰지도 쌓지도 않는다")
	void withoutPrepaid() {
		CreditPlan plan = CreditPlan.of(STORE, 8_000, 15_000, FEE, 9_999, false);

		assertThat(plan.usedPoint()).isZero();
		assertThat(plan.earnedPoint()).isZero();
		assertThat(plan.payAmount()).isEqualTo(10_000);   // 8,000 + 배달비
		assertThat(plan.balanceAfter()).isEqualTo(9_999); // 잔액 그대로
	}

	@ParameterizedTest(name = "items={0} min={1} balance={2}")
	@DisplayName("어떤 조합에서도 돈이 생기거나 사라지지 않는다")
	@CsvSource({
		"8000, 15000, 0",
		"9000, 15000, 7000",
		"20000, 15000, 6000",
		"3000, 15000, 20000",
		"15000, 15000, 0",
		"12000, 15000, 4000",
		"0, 15000, 0",
		"30000, 10000, 30000",
		"3000, 15000, 17000",
		"9000, 15000, 3000",
	})
	void moneyIsConserved(int itemsTotal, int minOrderPrice, int balance) {
		CreditPlan plan = CreditPlan.of(STORE, itemsTotal, minOrderPrice, FEE, balance, true);

		int pointDelta = plan.balanceAfter() - plan.balanceBefore();

		// 낸 현금 = 받은 음식값 + 배달비 + 포인트 순증
		assertThat(plan.payAmount())
			.isEqualTo(itemsTotal + FEE + pointDelta);

		assertThat(plan.balanceAfter()).isNotNegative();
		assertThat(plan.usedPoint()).isLessThanOrEqualTo(balance);
		assertThat(plan.payAmount()).isNotNegative();
	}
}
