package mukkeu.mukkeu.credit.domain;

/**
 * 가게 한 곳의 결제를 채움 포인트까지 포함해 계산한 결과.
 *
 * 순수 계산이라 DB 도 스프링도 모른다. 돈이 틀리면 안 되는 규칙이 여기 한 곳에만 있어야
 * 서버·앱·테스트가 같은 답을 낸다.
 *
 * ── 계산 규칙 ──────────────────────────────────────────────
 *   base        = max(itemsTotal, minOrderPrice)   가게가 받을 음식값
 *   shortfall   = base - itemsTotal                음식 대신 포인트로 돌려받는 부분
 *   payable     = base + deliveryFee               이번 결제에 필요한 총액
 *   usedPoint   = min(balance, payable)            ★ 포인트를 먼저, 배달비까지 전부
 *   payAmount   = payable - usedPoint              실제로 낼 현금
 *   earnedPoint = shortfall
 *   newBalance  = balance - usedPoint + earnedPoint
 *
 * ★ 포인트를 payable(음식값 + 배달비) 까지 쓴다.
 *   미달분에 포인트를 쓰면 그만큼 증발하는 것 아니냐는 의심이 들지만, 그렇지 않다.
 *   미달분은 shortfall 로 그대로 다시 적립되기 때문에 낸 만큼 돌아온다.
 *   음식값까지만 쓰게 막으면 오히려 현금이 더 나가고 잔액이 불어난다 —
 *   잔액 20,000 / 3,000원어치 주문에서 잔액이 29,000 이 되는 식이다.
 *
 * ★ 배달비도 포인트로 낸다.
 *   "배달비는 최소주문 판정에서 뺀다" 는 규칙과 혼동하기 쉬운데 둘은 별개다.
 *   판정은 여전히 itemsTotal 기준이고, 포인트는 그 가게에 미리 낸 돈이므로
 *   같은 가게에 내는 배달비에 못 쓸 이유가 없다. 잔액이 넉넉하면 결제액이 0 이 된다.
 *
 * ── 이 계산이 지키는 항등식 ────────────────────────────────
 *   낸 현금 = 받은 음식값 + 배달비 + 포인트 순증
 *   이게 깨지면 어딘가에서 돈이 생기거나 사라진다. CreditPlanTest 가 검사한다.
 *
 * ── 검산 (배달비 2,000 기준) ──────────────────────────────
 *   1회차    items  8,000 / min 15,000 / bal      0
 *            → used      0, earned  7,000, pay 17,000, 잔액 →  7,000
 *   2회차    items  9,000 / min 15,000 / bal  7,000
 *            → used  7,000, earned  6,000, pay 10,000, 잔액 →  6,000
 *   충족     items 20,000 / min 15,000 / bal  6,000
 *            → used  6,000, earned      0, pay 16,000, 잔액 →      0
 *   잔액과다 items  3,000 / min 15,000 / bal 20,000
 *            → used 17,000, earned 12,000, pay      0, 잔액 → 15,000  (결제 0원)
 *
 * 잔액이 충분하면 잔액은 항상 줄어든다(= itemsTotal + deliveryFee 만큼). 늘어나는 것은 잔액이
 * 미달분보다 적을 때뿐이고, 그때는 실제로 선불이 필요한 상황이다.
 */
public record CreditPlan(

	Long restaurantId,

	/** 담은 음식값. 최소주문 판정 기준이다(배달비는 빼고 본다). */
	int itemsTotal,

	/** 가게가 받을 음식값. 최소주문에 미달하면 최소주문 금액이 된다. */
	int base,

	/** 음식 대신 포인트로 돌려받는 금액. 미달이 없으면 0. */
	int shortfall,

	int usedPoint,
	int earnedPoint,

	/** 이번 결제에 필요한 총액(base + deliveryFee). 포인트를 쓰기 전 금액이다. */
	int payable,

	int deliveryFee,

	/** 이 가게에 실제로 결제되는 금액. */
	int payAmount,

	int balanceBefore,
	int balanceAfter
) {

	/**
	 * @param usePrepaid false 면 포인트를 쓰지도 쌓지도 않는다. 이때 미달이면
	 *                   호출자가 먼저 막아야 한다 — 이 계산은 판단하지 않는다.
	 */
	public static CreditPlan of(Long restaurantId, int itemsTotal, int minOrderPrice,
		int deliveryFee, int balance, boolean usePrepaid) {

		if (!usePrepaid) {
			int plain = itemsTotal + deliveryFee;
			return new CreditPlan(restaurantId, itemsTotal, itemsTotal, 0, 0, 0,
				plain, deliveryFee, plain, balance, balance);
		}

		int base = Math.max(itemsTotal, minOrderPrice);
		int shortfall = base - itemsTotal;
		int payable = base + deliveryFee;

		// 포인트 우선. 배달비까지 포함해 낼 수 있는 만큼 전부 쓴다.
		int usedPoint = Math.min(balance, payable);
		int earnedPoint = shortfall;

		return new CreditPlan(
			restaurantId, itemsTotal, base, shortfall,
			usedPoint, earnedPoint,
			payable, deliveryFee, payable - usedPoint,
			balance, balance - usedPoint + earnedPoint);
	}

	/** 포인트가 실제로 움직였는가. 안 움직였으면 잔액 행을 건드릴 필요가 없다. */
	public boolean touchesCredit() {
		return usedPoint > 0 || earnedPoint > 0;
	}

	/** 잔액 순증감. 음수면 이번 결제로 포인트가 줄었다는 뜻이다. */
	public int balanceDelta() {
		return balanceAfter - balanceBefore;
	}
}
