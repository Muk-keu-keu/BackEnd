package mukkeu.mukkeu.restaurant.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.domain.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RestaurantRepositoryAdapter implements RestaurantRepository {

	private final RestaurantJpaRepository restaurantJpaRepository;

	@Override
	public Optional<Restaurant> findById(Long id) {
		return restaurantJpaRepository.findById(id);
	}

	@Override
	public List<Restaurant> findAllByIdIn(List<Long> ids) {
		return restaurantJpaRepository.findAllByIdIn(ids);
	}

	@Override
	public List<Restaurant> findInBox(double minLat, double maxLat, double minLng, double maxLng) {
		return restaurantJpaRepository.findAllByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
	}

	@Override
	public List<Restaurant> findByBrandNameInBox(String brandName,
		double minLat, double maxLat, double minLng, double maxLng) {
		return restaurantJpaRepository.findAllByBrandNameAndLatBetweenAndLngBetween(
			brandName, minLat, maxLat, minLng, maxLng);
	}
}
