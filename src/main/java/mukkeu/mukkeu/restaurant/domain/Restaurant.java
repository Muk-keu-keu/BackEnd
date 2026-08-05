package mukkeu.mukkeu.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 식당. 시드 SQL 로 손수 넣는 데이터라 앱이 생성하지 않는다.
 * 조회 전용으로만 쓴다.
 */
@Entity
@Getter
@Table(name = "restaurant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant {

	@Id
	@Column(name = "restaurant_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 화면 표시용 원본 상호명. '교촌치킨 성수점' */
	@Column(nullable = false, length = 200)
	private String name;

	/** 브랜드 중복 제거용 코드. 'KYOCHON' */
	@Column(name = "brand_code", nullable = false, length = 30)
	private String brandCode;

	/** 영상 매칭용 정제값. '교촌치킨' — 지점명이 섞이면 매칭이 안 된다. */
	@Column(name = "brand_name", nullable = false, length = 100)
	private String brandName;

	@Column(name = "branch_name", length = 50)
	private String branchName;

	@Enumerated(EnumType.STRING)
	@Column(name = "food_category", nullable = false, length = 20)
	private FoodCategory foodCategory;

	@Column(nullable = false, length = 50)
	private String area;

	@Column(length = 50)
	private String sigungu;

	@Column(length = 300)
	private String address;

	/** 반경 5km 박스 필터의 기준. */
	@Column(nullable = false)
	private Double lat;

	@Column(nullable = false)
	private Double lng;

	private Double rating;

	@Column(name = "review_count")
	private Integer reviewCount;

	@Column(name = "min_order_price")
	private Integer minOrderPrice;

	@Column(name = "delivery_fee")
	private Integer deliveryFee;

	/** 가게가 표기하는 배달 예상시간. maxDeliveryMin 필터가 이 값을 본다. */
	@Column(name = "delivery_min")
	private Integer deliveryMin;

	/** 조리시간. etaMin = 카카오 이동시간 + prep_min */
	@Column(name = "prep_min")
	private Integer prepMin;

	@Column(name = "image_url", length = 500)
	private String imageUrl;
}
