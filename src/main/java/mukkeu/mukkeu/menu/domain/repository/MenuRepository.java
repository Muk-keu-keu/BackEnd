package mukkeu.mukkeu.menu.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.menu.domain.Menu;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface MenuRepository {

	Optional<Menu> findById(Long id);

	List<Menu> findAllByIdIn(List<Long> ids);

	/** 가게 전체 메뉴 — 메뉴 수정하기 화면 */
	List<Menu> findAllByRestaurantId(Long restaurantId);

	List<Menu> findAllByRestaurantIdIn(List<Long> restaurantIds);

	/**
	 * 벡터 유사도 검색. 가까운 순으로 menu_id 를 돌려준다.
	 * 엔티티가 embedding 을 담지 못하므로 id 만 받고, 본문은 findAllByIdIn 으로 읽는다.
	 *
	 * @param queryText   임베딩할 검색 문장
	 * @param restaurantIds 반경 안에서 이미 걸러진 가게들
	 * @param maxSpiceRank  사용자가 허용한 맵기 상한(0/1/2). null 이면 제한 없음
	 * @param excludeMeat   고기 제외 여부
	 */
	List<Long> searchSimilarMenuIds(String queryText, List<Long> restaurantIds,
		Integer maxSpiceRank, boolean excludeMeat, int limit);
}
