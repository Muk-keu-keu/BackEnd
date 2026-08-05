package mukkeu.mukkeu.restaurant.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.restaurant.domain.Restaurant;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface RestaurantRepository {

	Optional<Restaurant> findById(Long id);

	List<Restaurant> findAllByIdIn(List<Long> ids);

	/**
	 * 배달권역 박스 필터. 반경 원이 아니라 사각형으로 먼저 거른다.
	 * 인덱스(ix_rest_geo)를 타기 위해서다. 정확한 거리는 후처리한다.
	 */
	List<Restaurant> findInBox(double minLat, double maxLat, double minLng, double maxLng);

	/** Path A — 영상에서 뽑은 브랜드명으로 반경 안의 지점을 찾는다. */
	List<Restaurant> findByBrandNameInBox(String brandName,
		double minLat, double maxLat, double minLng, double maxLng);
}
