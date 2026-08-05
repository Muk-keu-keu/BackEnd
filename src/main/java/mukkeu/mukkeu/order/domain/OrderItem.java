package mukkeu.mukkeu.order.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
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
 * 주문 항목 — 장바구니 한 줄.
 *
 * 가게 정보는 여기 없다. 행 하나가 곧 가게 하나라 orders 에 한 번만 있으면 된다.
 * 메뉴 이름·가격·설명·사진을 복사해 두는 이유는 원본이 바뀌거나 지워져도
 * 내역이 그대로여야 하기 때문이다. 조인 없이 내역을 그릴 수 있는 건 덤이다.
 */
@Entity
@Getter
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	@Id
	@Column(name = "order_item_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	// ── 메뉴 스냅샷 ──
	@Column(name = "menu_id", nullable = false)
	private Long menuId;

	@Column(name = "menu_name", nullable = false, length = 200)
	private String menuName;

	// 메뉴 설명
	@Column(name = "menu_desc", length = 500)
	private String menuDesc;

	@Column(name = "menu_image_url", length = 500)
	private String menuImageUrl;

	/** 주문 시점 메뉴 정가 */
	@Column(name = "unit_price", nullable = false)
	private Integer unitPrice;

	@Column(nullable = false)
	private Integer quantity;

	// ── 사용자가 고른 것 ──
	/** 맵기 조절이 불가능한 메뉴면 null */
	@Enumerated(EnumType.STRING)
	@Column(name = "selected_spice", length = 10)
	private SpiceLevel selectedSpice;

	/**
	 * 고른 옵션만 담은 JSON 배열 문자열.
	 *   [{"group":null,"name":"분모자","price":2000}]
	 * menu.options 와 같은 모양이라 프론트가 변환할 것이 없다.
	 */
	@Column(name = "selected_options", length = 2000)
	private String selectedOptions;

	@Column(name = "options_price", nullable = false)
	private Integer optionsPrice;

	/** (unit_price + options_price) * quantity */
	@Column(name = "line_total", nullable = false)
	private Integer lineTotal;

	@Builder
	private OrderItem(Long orderId, Long menuId, String menuName, String menuDesc,
		String menuImageUrl, Integer unitPrice, Integer quantity, SpiceLevel selectedSpice,
		String selectedOptions, Integer optionsPrice, Integer lineTotal) {
		this.orderId = orderId;
		this.menuId = menuId;
		this.menuName = menuName;
		this.menuDesc = menuDesc;
		this.menuImageUrl = menuImageUrl;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.selectedSpice = selectedSpice;
		this.selectedOptions = selectedOptions;
		this.optionsPrice = optionsPrice;
		this.lineTotal = lineTotal;
	}

	/** orders 를 먼저 저장해 order_id 를 받은 뒤 채운다. */
	public void assignOrderId(Long orderId) {
		this.orderId = orderId;
	}
}
