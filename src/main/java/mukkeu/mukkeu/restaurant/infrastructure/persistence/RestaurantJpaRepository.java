package mukkeu.mukkeu.restaurant.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.restaurant.domain.Restaurant;

public interface RestaurantJpaRepository extends JpaRepository<Restaurant, Long> {

	List<Restaurant> findAllByIdIn(List<Long> ids);

	List<Restaurant> findAllByLatBetweenAndLngBetween(
		Double minLat, Double maxLat, Double minLng, Double maxLng);
}
