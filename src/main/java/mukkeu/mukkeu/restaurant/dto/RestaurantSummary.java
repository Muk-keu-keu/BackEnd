package mukkeu.mukkeu.restaurant.dto;

import mukkeu.mukkeu.restaurant.domain.FoodCategory;

/**
 * 가게 카드 한 장. 먹방요기 결과와 메뉴판 조회가 같은 모양을 쓴다.
 *
 * 원래는 AnalysisResponse.StoreResponse 안에만 있었다. 메뉴판 조회에도 같은 값이
 * 필요해지면서 밖으로 꺼냈다. 두 곳에 각각 두면 한쪽에 평점을 추가하고 다른 쪽을
 * 잊는 순간 화면마다 다른 정보가 뜬다.
 *
 * 값의 출처가 두 갈래다.
 *   테이블에 그대로 있는 것 : name, rating, reviewCount, deliveryFee, minOrderPrice, imageUrl
 *   요청마다 계산하는 것   : distanceKm, etaMin — 둘 다 로그인한 사용자 좌표가 기준이라
 *                            같은 가게라도 사람마다 다른 값이 나간다.
 */
public record RestaurantSummary(
	Long restaurantId,
	String name,
	FoodCategory foodCategory,
	String area,
	String address,

	/** 평점(5점 만점). 리뷰가 없으면 null 이다. 0.0 으로 채우지 않는다. */
	Double rating,

	/** 리뷰 수. 평점 4.9 가 3건인지 3000건인지에 따라 의미가 완전히 다르다. */
	Integer reviewCount,

	/**
	 * 도착 예정 시간(분) = 가게 조리시간(prep_min) + 카카오 실측 이동시간.
	 * 길찾기가 실패하면 가게가 적어 둔 delivery_min 이 그대로 나간다.
	 */
	Integer etaMin,

	Integer deliveryFee,
	Integer minOrderPrice,

	/** 사용자 좌표에서 가게까지 직선거리(km), 소수 둘째 자리 반올림. */
	Double distanceKm,

	String imageUrl
) {
}
