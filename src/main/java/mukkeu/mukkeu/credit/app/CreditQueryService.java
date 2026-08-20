package mukkeu.mukkeu.credit.app;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.credit.domain.UserStoreCredit;
import mukkeu.mukkeu.credit.domain.repository.UserStoreCreditRepository;
import mukkeu.mukkeu.credit.dto.CreditBalanceResponse;
import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.domain.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;

/**
 * 화면에 보여줄 잔액을 만든다.
 *
 * CreditService 와 나눠 둔 이유는 의존 방향이다. 결제 경로(CreditService)는 잔액만 알면
 * 되지만 화면은 가게 이름·사진까지 필요해서 RestaurantRepository 를 봐야 한다.
 * 한 클래스에 두면 결제가 가게 정보에 딸려 들어간다.
 */
@Service
@RequiredArgsConstructor
public class CreditQueryService {

	private final UserStoreCreditRepository userStoreCreditRepository;
	private final RestaurantRepository restaurantRepository;

	@Transactional(readOnly = true)
	public CreditBalanceResponse getMyCredits(Long userId) {

		List<UserStoreCredit> credits = userStoreCreditRepository.findAllPositiveByUserId(userId);
		if (credits.isEmpty()) {
			return new CreditBalanceResponse(List.of());
		}

		// 가게 이름을 한 번에 읽는다. 잔액마다 findById 를 부르면 N+1 이다.
		Map<Long, Restaurant> byId = restaurantRepository
			.findAllByIdIn(credits.stream().map(UserStoreCredit::getRestaurantId).toList()).stream()
			.collect(Collectors.toMap(Restaurant::getId, r -> r, (a, b) -> a));

		// 잔액이 큰 가게가 먼저다. 쓸 수 있는 곳부터 눈에 들어와야 소진으로 이어진다.
		List<CreditBalanceResponse.StoreCredit> items = credits.stream()
			.sorted(Comparator.comparing(UserStoreCredit::getBalance).reversed())
			.map(c -> toItem(c.getRestaurantId(), c.getBalance(), byId.get(c.getRestaurantId())))
			.toList();

		return new CreditBalanceResponse(items);
	}

	/**
	 * 가게 한 곳의 잔액.
	 *
	 * 거래가 없으면 balance 0 을 돌려준다. 404 로 만들면 프론트가 "포인트 없음" 과
	 * "가게 없음" 을 구분해 처리해야 하는데, 화면에서는 둘 다 배지를 숨기는 같은 동작이다.
	 */
	@Transactional(readOnly = true)
	public CreditBalanceResponse.StoreCredit getOne(Long userId, Long restaurantId) {

		int balance = userStoreCreditRepository.find(userId, restaurantId)
			.map(UserStoreCredit::getBalance)
			.orElse(0);

		Restaurant store = restaurantRepository.findById(restaurantId).orElse(null);
		return toItem(restaurantId, balance, store);
	}

	private CreditBalanceResponse.StoreCredit toItem(Long restaurantId, int balance, Restaurant store) {
		return new CreditBalanceResponse.StoreCredit(
			restaurantId,
			store == null ? null : store.getName(),
			balance,
			store == null ? null : store.getImageUrl());
	}
}
