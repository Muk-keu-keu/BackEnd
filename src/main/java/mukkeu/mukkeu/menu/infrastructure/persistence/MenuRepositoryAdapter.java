package mukkeu.mukkeu.menu.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.menu.domain.Menu;
import mukkeu.mukkeu.menu.domain.MenuMatch;
import mukkeu.mukkeu.menu.domain.repository.MenuRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryAdapter implements MenuRepository {

	private final MenuJpaRepository menuJpaRepository;
	private final MenuVectorRepository menuVectorRepository;

	@Override
	public Optional<Menu> findById(Long id) {
		return menuJpaRepository.findById(id);
	}

	@Override
	public List<Menu> findAllByIdIn(List<Long> ids) {
		return ids.isEmpty() ? List.of() : menuJpaRepository.findAllByIdIn(ids);
	}

	@Override
	public List<Menu> findAllByRestaurantId(Long restaurantId) {
		return menuJpaRepository.findAllByRestaurantId(restaurantId);
	}

	@Override
	public List<Menu> findAllByRestaurantIdIn(List<Long> restaurantIds) {
		return restaurantIds.isEmpty() ? List.of() : menuJpaRepository.findAllByRestaurantIdIn(restaurantIds);
	}

	@Override
	public List<MenuMatch> searchSimilar(String queryText, List<Long> restaurantIds, Integer maxSpiceRank, boolean excludeMeat, int limit) {
		return menuVectorRepository.search(queryText, restaurantIds, maxSpiceRank, excludeMeat, limit);
	}
}
