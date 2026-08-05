package mukkeu.mukkeu.menu.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.menu.domain.Menu;

public interface MenuJpaRepository extends JpaRepository<Menu, Long> {

	List<Menu> findAllByIdIn(List<Long> ids);

	List<Menu> findAllByRestaurantId(Long restaurantId);

	List<Menu> findAllByRestaurantIdIn(List<Long> restaurantIds);
}
