package mukkeu.mukkeu.order.domain.repository;

import java.util.List;

import mukkeu.mukkeu.order.domain.Order;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface OrderRepository {

	/** checkout_seq 에서 결제 묶음 번호를 하나 뽑는다. 결제 한 번에 한 번만 호출한다. */
	Long nextCheckoutId();

	Order save(Order order);

	List<Order> saveAll(List<Order> orders);

	/** 내역 상세. 행이 비면 없는 주문이든 남의 주문이든 똑같이 404 로 처리한다. */
	List<Order> findAllByCheckoutIdAndUserId(Long checkoutId, Long userId);

	/**
	 * 내역 목록 1단계 — 이번 페이지에 보여줄 결제 번호만 뽑는다.
	 * 행 기준으로 자르면 묶음이 반으로 잘려 카드에 가게 하나만 뜬다.
	 */
	List<Long> findCheckoutIdsByUserId(Long userId, int size);

	/** 내역 목록 2단계 — 그 결제들의 모든 가게 행 */
	List<Order> findAllByCheckoutIdIn(List<Long> checkoutIds);

	/** 족보 글을 쓸 자격이 있는지 — 그 결제가 내 것으로 존재하나 */
	boolean existsByCheckoutIdAndUserId(Long checkoutId, Long userId);
}
