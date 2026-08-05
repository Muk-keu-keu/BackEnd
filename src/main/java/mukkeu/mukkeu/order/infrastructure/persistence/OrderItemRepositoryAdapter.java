package mukkeu.mukkeu.order.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.order.domain.OrderItem;
import mukkeu.mukkeu.order.domain.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements OrderItemRepository {

	private final OrderItemJpaRepository orderItemJpaRepository;

	@Override
	public List<OrderItem> saveAll(List<OrderItem> items) {
		return orderItemJpaRepository.saveAll(items);
	}

	@Override
	public List<OrderItem> findAllByOrderId(Long orderId) {
		return orderItemJpaRepository.findAllByOrderId(orderId);
	}

	@Override
	public List<OrderItem> findAllByOrderIdIn(List<Long> orderIds) {
		return orderItemJpaRepository.findAllByOrderIdIn(orderIds);
	}
}
