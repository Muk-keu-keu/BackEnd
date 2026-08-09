package mukkeu.mukkeu.restaurant.app;

import mukkeu.mukkeu.restaurant.domain.Restaurant;
import mukkeu.mukkeu.restaurant.dto.RestaurantSummary;

/**
 * Restaurant 엔티티 → 화면용 가게 카드.
 *
 * 먹방요기와 메뉴판 조회가 같은 변환을 쓰게 하려고 한 곳에 모았다. 이게 없으면
 * 두 서비스가 각자 new RestaurantSummary(...) 를 쓰고, 인자 순서가 열두 개라
 * 한쪽에서 area 와 address 를 바꿔 넣어도 컴파일이 통과한다(둘 다 String 이다).
 *
 * distanceKm 과 etaMin 은 사용자 좌표가 있어야 나오는 값이라 인자로 받는다.
 * 엔티티만 보고는 계산할 수 없다.
 */
public final class RestaurantSummaryFactory {

	private RestaurantSummaryFactory() {
	}

	/**
	 * @param distanceKm 사용자 좌표로부터의 직선거리. 여기서 소수 둘째 자리로 반올림한다.
	 * @param etaMin     도착 예정(분). 카카오 실측이 있으면 그 값, 없으면 delivery_min.
	 */
	public static RestaurantSummary of(Restaurant store, Double distanceKm, Integer etaMin) {
		return new RestaurantSummary(
			store.getId(),
			store.getName(),
			store.getFoodCategory(),
			store.getArea(),
			store.getAddress(),
			store.getRating(),
			store.getReviewCount(),
			etaMin,
			store.getDeliveryFee(),
			store.getMinOrderPrice(),
			distanceKm == null ? null : Math.round(distanceKm * 100) / 100.0,
			store.getImageUrl());
	}
}
