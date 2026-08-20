package mukkeu.mukkeu.credit.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.credit.domain.UserStoreCredit;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface UserStoreCreditRepository {

	/** 화면 표시용. 잠그지 않는다. */
	Optional<UserStoreCredit> find(Long userId, Long restaurantId);

	/** 잔액이 남아 있는 것만. 0 인 행을 화면에 뿌릴 이유가 없다. */
	List<UserStoreCredit> findAllPositiveByUserId(Long userId);

	List<UserStoreCredit> findAllByUserIdAndRestaurantIdIn(Long userId, List<Long> restaurantIds);

	/**
	 * 결제용. 행을 잠그고 읽는다.
	 *
	 * 잠그지 않으면 결제 버튼을 두 번 누른 순간 두 트랜잭션이 같은 잔액을 읽고
	 * 각자 차감해서 잔액이 한 번만 줄어든다. 포인트가 공짜로 복제되는 셈이다.
	 */
	Optional<UserStoreCredit> findForUpdate(Long userId, Long restaurantId);

	UserStoreCredit save(UserStoreCredit credit);
}
