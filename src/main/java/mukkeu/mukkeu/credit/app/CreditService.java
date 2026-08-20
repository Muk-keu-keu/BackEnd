package mukkeu.mukkeu.credit.app;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.credit.domain.CreditPlan;
import mukkeu.mukkeu.credit.domain.UserStoreCredit;
import mukkeu.mukkeu.credit.domain.repository.UserStoreCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채움 포인트. 잔액 조회와 결제 시 반영을 맡는다.
 *
 * 금액 계산은 CreditPlan 이 하고 이 서비스는 하지 않는다. 계산을 서비스에 두면
 * 스프링 컨텍스트 없이 검산할 수 없고, 앱과 규칙이 어긋났을 때 어디를 봐야 하는지
 * 흩어진다.
 *
 * ── 왜 여기서 트랜잭션을 열지 않는가 ──────────────────────
 * apply() 는 OrderService.create() 안에서 불린다. 그쪽이 이미 트랜잭션을 열고 있고,
 * 포인트 차감만 따로 커밋되면 주문이 실패해도 잔액은 줄어든 채로 남는다.
 * 그래서 전파를 받아 쓰기만 하고 새로 열지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

	private final UserStoreCreditRepository userStoreCreditRepository;

	// ────────────────────────────────────────────────────────
	//  조회
	// ────────────────────────────────────────────────────────

	/** 잔액이 남은 가게만. 잔액 0 인 행은 화면에 그릴 것이 없다. */
	@Transactional(readOnly = true)
	public List<UserStoreCredit> balances(Long userId) {
		if (userId == null) {
			return List.of();
		}
		return userStoreCreditRepository.findAllPositiveByUserId(userId);
	}

	/** 가게 한 곳의 잔액. 비로그인이거나 거래가 없으면 0. */
	@Transactional(readOnly = true)
	public int balanceOf(Long userId, Long restaurantId) {
		if (userId == null || restaurantId == null) {
			return 0;
		}
		return userStoreCreditRepository.find(userId, restaurantId)
			.map(UserStoreCredit::getBalance)
			.orElse(0);
	}

	/** 여러 가게 잔액을 한 번에. 장바구니처럼 가게가 여럿일 때 N+1 을 피한다. */
	@Transactional(readOnly = true)
	public Map<Long, Integer> balancesOf(Long userId, List<Long> restaurantIds) {
		if (userId == null || restaurantIds == null || restaurantIds.isEmpty()) {
			return Map.of();
		}
		return userStoreCreditRepository
			.findAllByUserIdAndRestaurantIdIn(userId, restaurantIds).stream()
			.collect(Collectors.toMap(UserStoreCredit::getRestaurantId,
				UserStoreCredit::getBalance, (a, b) -> a, LinkedHashMap::new));
	}

	// ────────────────────────────────────────────────────────
	//  결제 반영
	// ────────────────────────────────────────────────────────

	/**
	 * 결제 한 건의 포인트를 계산하고 잔액에 반영한다.
	 *
	 * 가게마다 행을 잠그고 읽으므로(FOR UPDATE) 같은 사용자가 결제를 두 번 눌러도
	 * 잔액이 이중으로 차감되지 않는다. 잠그는 순서를 restaurantId 오름차순으로
	 * 고정한 이유는 교착 때문이다 — 가게 A·B 를 담은 두 결제가 서로 반대 순서로
	 * 잠그면 둘 다 상대를 기다린다.
	 *
	 * @param inputs 가게별 결제 입력. 담긴 순서와 무관하게 잠금 순서는 여기서 정한다.
	 * @return 가게 id → 계산 결과. 입력에 있던 가게는 전부 들어 있다.
	 */
	@Transactional
	public Map<Long, CreditPlan> apply(Long userId, List<StoreInput> inputs, boolean usePrepaid) {

		Map<Long, CreditPlan> plans = new LinkedHashMap<>();

		List<StoreInput> ordered = inputs.stream()
			.sorted(Comparator.comparing(StoreInput::restaurantId))
			.toList();

		for (StoreInput input : ordered) {

			if (!usePrepaid) {
				// 포인트를 쓰지도 쌓지도 않는다. 잔액 행을 잠글 이유도 없다.
				plans.put(input.restaurantId(), CreditPlan.of(input.restaurantId(),
					input.itemsTotal(), input.minOrderPrice(), input.deliveryFee(), 0, false));
				continue;
			}

			UserStoreCredit credit = userStoreCreditRepository
				.findForUpdate(userId, input.restaurantId())
				.orElseGet(() -> new UserStoreCredit(userId, input.restaurantId()));

			CreditPlan plan = CreditPlan.of(input.restaurantId(), input.itemsTotal(),
				input.minOrderPrice(), input.deliveryFee(), credit.getBalance(), true);

			if (plan.touchesCredit()) {
				credit.use(plan.usedPoint());
				credit.earn(plan.earnedPoint());
				userStoreCreditRepository.save(credit);

				log.info("포인트 반영 user={} store={} 사용={} 적립={} 잔액={}→{}",
					userId, input.restaurantId(), plan.usedPoint(), plan.earnedPoint(),
					plan.balanceBefore(), plan.balanceAfter());
			}

			plans.put(input.restaurantId(), plan);
		}

		// 입력 순서대로 돌려준다. 호출자가 stores 순서와 맞춰 쓰기 때문이다.
		return inputs.stream().collect(Collectors.toMap(StoreInput::restaurantId,
			s -> plans.get(s.restaurantId()), (a, b) -> a, LinkedHashMap::new));
	}

	/** apply() 의 입력. 가게 하나의 금액과 조건. */
	public record StoreInput(
		Long restaurantId,
		int itemsTotal,
		int minOrderPrice,
		int deliveryFee
	) {
	}
}
