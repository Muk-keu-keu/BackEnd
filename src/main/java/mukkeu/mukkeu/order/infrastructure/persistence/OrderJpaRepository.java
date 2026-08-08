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
	 *
	 * 커서를 시각이 아니라 checkout_id 로 잡는다. checkout_seq 가 계속 증가하므로
	 * 번호가 큰 것이 곧 최신이고, 번호는 중복이 없어 같은 초에 두 건이 들어와도
	 * 한 건이 건너뛰거나 두 번 나오는 일이 없다.
	 *
	 * 첫 페이지는 cursor 에 Long.MAX_VALUE 를 넣어 부른다. SQL 에 NULL 분기를 두면
	 * 바인드 타입이 모호해져 ORA-00932 가 날 수 있어서, 자바 쪽에서 값을 바꿔 넣는다.
	 */
	@Query(value = """
		SELECT   checkout_id
		FROM     orders
		WHERE    user_id = :userId
		  AND    checkout_id < :cursor
		GROUP BY checkout_id
		ORDER BY checkout_id DESC
		FETCH FIRST :size ROWS ONLY
		""", nativeQuery = true)
	List<Long> findCheckoutIdsByUserId(@Param("userId") Long userId,
		@Param("cursor") Long cursor, @Param("size") int size);
}
