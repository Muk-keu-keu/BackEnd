package mukkeu.mukkeu.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mukkeu.mukkeu.order.domain.Order;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

	@Query(value = "SELECT checkout_seq.NEXTVAL FROM dual", nativeQuery = true)
	Long nextCheckoutId();

	List<Order> findAllByCheckoutIdAndUserId(Long checkoutId, Long userId);

	List<Order> findAllByCheckoutIdIn(List<Long> checkoutIds);

	boolean existsByCheckoutIdAndUserId(Long checkoutId, Long userId);

	/**
	 * 결제 묶음 번호만 최신순으로. 카드 단위로 페이지를 자르기 위해서다.
	 * (커서 페이지네이션으로 바꿀 때 created_at 조건을 여기에 더한다)
	 */
	@Query(value = """
		SELECT   checkout_id
		FROM     orders
		WHERE    user_id = :userId
		GROUP BY checkout_id
		ORDER BY MAX(created_at) DESC
		FETCH FIRST :size ROWS ONLY
		""", nativeQuery = true)
	List<Long> findCheckoutIdsByUserId(@Param("userId") Long userId, @Param("size") int size);
}
