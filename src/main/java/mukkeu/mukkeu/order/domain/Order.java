package mukkeu.mukkeu.order.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문. 행 1개 = 가게 1곳, checkout_id 1개 = 결제 1번.
 *
 * 배달은 가게 단위로 일어나므로(라이더도 도착시간도 배달비도 따로) 행을 나누고,
 * 사용자가 결제 버튼을 한 번 누른 사실은 checkout_id 로 남긴다.
 * 엽떡 + 교촌을 한 번에 결제하면 행이 2개 생기고 checkout_id 가 같다.
 *
 * ★ API 에는 checkoutId 만 나간다. order_id 는 내부 저장용이다.
 * ★ 전체 총액 컬럼은 없다. SUM(total_price) 로 구한다.
 * ★ 금액·이름은 전부 결제 시점 스냅샷이다. 원본이 바뀌어도 내역은 안 변한다.
 *
 * ★ 주문 시각은 BaseEntity 의 created_at 이다. 행이 생긴 시각과 같은 값이라
 *   ordered_at 을 따로 두지 않는다. API 로는 orderedAt 이름으로 내보낸다.
 */
@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false))
public class Order extends BaseEntity {

	@Id
	@Column(name = "order_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 결제 묶음. 같은 결제로 생긴 행들은 이 값이 같다. checkout_seq 로 발급. */
	@Column(name = "checkout_id", nullable = false)
	private Long checkoutId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(name = "restaurant_name", nullable = false, length = 200)
	private String restaurantName;

	@Column(name = "delivery_fee", nullable = false)
	private Integer deliveryFee;

	// ── 영상 출처. 같은 checkout_id 행끼리 값이 같다(의도된 중복) ──
	@Enumerated(EnumType.STRING)
	@Column(name = "source_platform", length = 20)
	private SourcePlatform sourcePlatform;

	@Column(name = "source_url", length = 1000)
	private String sourceUrl;

	@Column(name = "source_thumbnail", length = 1000)
	private String sourceThumbnail;

	@Column(name = "source_title", length = 300)
	private String sourceTitle;

	// ── 금액 ──
	/** 이 가게 메뉴 + 옵션 합 */
	@Column(name = "items_total", nullable = false)
	private Integer itemsTotal;

	/** items_total + delivery_fee */
	@Column(name = "total_price", nullable = false)
	private Integer totalPrice;

	@Builder
	private Order(Long checkoutId, Long userId, Long restaurantId, String restaurantName,
		Integer deliveryFee, SourcePlatform sourcePlatform, String sourceUrl,
		String sourceThumbnail, String sourceTitle, Integer itemsTotal, Integer totalPrice) {
		this.checkoutId = checkoutId;
		this.userId = userId;
		this.restaurantId = restaurantId;
		this.restaurantName = restaurantName;
		this.deliveryFee = deliveryFee;
		this.sourcePlatform = sourcePlatform;
		this.sourceUrl = sourceUrl;
		this.sourceThumbnail = sourceThumbnail;
		this.sourceTitle = sourceTitle;
		this.itemsTotal = itemsTotal;
		this.totalPrice = totalPrice;
	}
}
