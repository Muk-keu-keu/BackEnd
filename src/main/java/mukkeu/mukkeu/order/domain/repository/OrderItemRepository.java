package mukkeu.mukkeu.order.domain.repository;

import java.util.List;

import mukkeu.mukkeu.order.domain.OrderItem;

public interface OrderItemRepository {

	List<OrderItem> saveAll(List<OrderItem> items);

	List<OrderItem> findAllByOrderId(Long orderId);

	List<OrderItem> findAllByOrderIdIn(List<Long> orderIds);
}
