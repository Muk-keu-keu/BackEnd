package mukkeu.mukkeu.credit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.credit.domain.UserStoreCredit;
import mukkeu.mukkeu.credit.domain.repository.UserStoreCreditRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserStoreCreditRepositoryAdapter implements UserStoreCreditRepository {

	private final UserStoreCreditJpaRepository userStoreCreditJpaRepository;

	@Override
	public Optional<UserStoreCredit> find(Long userId, Long restaurantId) {
		return userStoreCreditJpaRepository.findByUserIdAndRestaurantId(userId, restaurantId);
	}

	@Override
	public List<UserStoreCredit> findAllPositiveByUserId(Long userId) {
		return userStoreCreditJpaRepository.findAllByUserIdAndBalanceGreaterThan(userId, 0);
	}

	@Override
	public List<UserStoreCredit> findAllByUserIdAndRestaurantIdIn(Long userId, List<Long> restaurantIds) {
		if (restaurantIds == null || restaurantIds.isEmpty()) {
			return List.of();
		}
		return userStoreCreditJpaRepository.findAllByUserIdAndRestaurantIdIn(userId, restaurantIds);
	}

	@Override
	public Optional<UserStoreCredit> findForUpdate(Long userId, Long restaurantId) {
		return userStoreCreditJpaRepository.findForUpdate(userId, restaurantId);
	}

	@Override
	public UserStoreCredit save(UserStoreCredit credit) {
		return userStoreCreditJpaRepository.save(credit);
	}
}
