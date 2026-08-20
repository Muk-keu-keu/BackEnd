package mukkeu.mukkeu.credit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import mukkeu.mukkeu.credit.domain.UserStoreCredit;
import mukkeu.mukkeu.credit.domain.UserStoreCreditId;

public interface UserStoreCreditJpaRepository
	extends JpaRepository<UserStoreCredit, UserStoreCreditId> {

	Optional<UserStoreCredit> findByUserIdAndRestaurantId(Long userId, Long restaurantId);

	List<UserStoreCredit> findAllByUserIdAndBalanceGreaterThan(Long userId, Integer balance);

	List<UserStoreCredit> findAllByUserIdAndRestaurantIdIn(Long userId, List<Long> restaurantIds);

	/**
	 * 결제 경로 전용. SELECT ... FOR UPDATE 로 행을 잠근다.
	 *
	 * 행이 없으면 잠글 것도 없다(첫 적립). 그 경우 호출자가 새 행을 만드는데,
	 * 같은 순간에 둘이 만들면 복합 PK 가 한쪽을 튕겨낸다 — 잔액이 틀리는 것보다 낫다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM UserStoreCredit c WHERE c.userId = :userId AND c.restaurantId = :restaurantId")
	Optional<UserStoreCredit> findForUpdate(@Param("userId") Long userId,
		@Param("restaurantId") Long restaurantId);
}
