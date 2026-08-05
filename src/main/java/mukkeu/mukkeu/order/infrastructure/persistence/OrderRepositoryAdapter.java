package mukkeu.mukkeu.order.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.order.domain.Order;
import mukkeu.mukkeu.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

	private final OrderJpaRepository orderJpaRepository;

	@Override
	public Long nextCheckoutId() {
		return orderJpaRepository.nextCheckoutId();
	}

	@Override
	public Order save(Order order) {
		return orderJpaRepository.save(order);
	}

	@Override
	public List<Order> saveAll(List<Order> orders) {
		return orderJpaRepository.saveAll(orders);
	}

	@Override
	public List<Order> findAllByCheckoutIdAndUserId(Long checkoutId, Long userId) {
		return orderJpaRepository.findAllByCheckoutIdAndUserId(checkoutId, userId);
	}

	@Override
	public List<Long> findCheckoutIdsByUserId(Long userId, int size) {
		return orderJpaRepository.findCheckoutIdsByUserId(userId, size);
	}

	@Override
	public List<Order> findAllByCheckoutIdIn(List<Long> checkoutIds) {
		return orderJpaRepository.findAllByCheckoutIdIn(checkoutIds);
	}

	@Override
	public boolean existsByCheckoutIdAndUserId(Long checkoutId, Long userId) {
		return orderJpaRepository.existsByCheckoutIdAndUserId(checkoutId, userId);
	}
}
