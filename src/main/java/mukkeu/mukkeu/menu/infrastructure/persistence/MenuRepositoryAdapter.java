package mukkeu.mukkeu.menu.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.menu.domain.Menu;
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
		return menuJpaRepository.findAllByIdIn(ids);
	}

	@Override
	public List<Menu> findAllByRestaurantId(Long restaurantId) {
		return menuJpaRepository.findAllByRestaurantId(restaurantId);
	}

	@Override
	public List<Menu> findAllByRestaurantIdIn(List<Long> restaurantIds) {
		return menuJpaRepository.findAllByRestaurantIdIn(restaurantIds);
	}

	@Override
	public List<Long> searchSimilarMenuIds(String queryText, List<Long> restaurantIds,
		Integer maxSpiceRank, boolean excludeMeat, int limit) {
		return menuVectorRepository.searchMenuIds(queryText, restaurantIds, maxSpiceRank, excludeMeat, limit);
	}
}
