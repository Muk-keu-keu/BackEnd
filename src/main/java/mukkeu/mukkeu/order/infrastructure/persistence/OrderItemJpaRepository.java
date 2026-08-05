package mukkeu.mukkeu.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.order.domain.OrderItem;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findAllByOrderId(Long orderId);

	List<OrderItem> findAllByOrderIdIn(List<Long> orderIds);
}
