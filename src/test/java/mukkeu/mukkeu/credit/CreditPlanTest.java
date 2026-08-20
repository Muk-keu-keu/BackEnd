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
	private static final int MIN = 14_000;

	@Test
	@DisplayName("잔액이 없으면 미달분을 전부 새로 선불한다")
	void noBalance() {
		CreditPlan plan = CreditPlan.of(STORE, 5_000, MIN, FEE, 0, true);

		assertThat(plan.base()).isEqualTo(MIN);          // 낮출 잔액이 없다
		assertThat(plan.usedPoint()).isZero();
		assertThat(plan.earnedPoint()).isEqualTo(9_000);
		assertThat(plan.payAmount()).isEqualTo(16_000);
		assertThat(plan.balanceAfter()).isEqualTo(9_000);
	}

	@Test
	@DisplayName("잔액이 미달분을 덮으면 최소주문이 낮아져 적립 없이 포인트만 쓴다")
	void balanceLowersMinimum() {
		CreditPlan plan = CreditPlan.of(STORE, 5_000, MIN, FEE, 9_000, true);

		// 실질 최소주문 = 14,000 - 9,000 = 5,000 → 5,000원어치면 충족이다
		assertThat(plan.base()).isEqualTo(5_000);
		assertThat(plan.shortfall()).isZero();
		assertThat(plan.earnedPoint()).isZero();
		assertThat(plan.usedPoint()).isEqualTo(7_000);   // 음식 5,000 + 배달비 2,000
		assertThat(plan.payAmount()).isZero();           // ★ 현금 0원
		assertThat(plan.balanceAfter()).isEqualTo(2_000);
	}

	@Test
	@DisplayName("크게 담으면 포인트를 다 쓰고 나머지를 현금으로 낸다")
	void largeOrder() {
		CreditPlan plan = CreditPlan.of(STORE, 30_000, MIN, FEE, 9_000, true);

		assertThat(plan.usedPoint()).isEqualTo(9_000);
		assertThat(plan.earnedPoint()).isZero();
		assertThat(plan.payAmount()).isEqualTo(23_000);  // 30,000 + 2,000 - 9,000
		assertThat(plan.balanceAfter()).isZero();
	}

	@Test
	@DisplayName("잔액이 미달분보다 적으면 낮춘 뒤 남은 만큼만 선불한다")
	void partialBalance() {
		CreditPlan plan = CreditPlan.of(STORE, 5_000, MIN, FEE, 3_000, true);

		// 실질 최소주문 = 14,000 - 3,000 = 11,000 → 6,000 부족
		assertThat(plan.base()).isEqualTo(11_000);
		assertThat(plan.earnedPoint()).isEqualTo(6_000);
		assertThat(plan.usedPoint()).isEqualTo(3_000);
		assertThat(plan.payAmount()).isEqualTo(10_000);
		assertThat(plan.balanceAfter()).isEqualTo(6_000);
	}

	@Test
	@DisplayName("usePrepaid=false 면 포인트를 쓰지도 쌓지도 않는다")
	void withoutPrepaid() {
		CreditPlan plan = CreditPlan.of(STORE, 5_000, MIN, FEE, 9_999, false);

		assertThat(plan.usedPoint()).isZero();
		assertThat(plan.earnedPoint()).isZero();
		assertThat(plan.payAmount()).isEqualTo(7_000);   // 5,000 + 배달비
		assertThat(plan.balanceAfter()).isEqualTo(9_999);
	}

	@ParameterizedTest(name = "items={0} min={1} balance={2}")
	@DisplayName("어떤 조합에서도 돈이 생기거나 사라지지 않는다")
	@CsvSource({
		"5000, 14000, 0",
		"5000, 14000, 9000",
		"5000, 14000, 3000",
		"30000, 14000, 9000",
		"14000, 14000, 9000",
		"0, 14000, 0",
		"3000, 15000, 20000",
		"20000, 15000, 6000",
		"12000, 15000, 4000",
		"30000, 10000, 30000",
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
