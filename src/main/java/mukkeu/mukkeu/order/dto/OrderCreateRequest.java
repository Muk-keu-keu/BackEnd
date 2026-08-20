package mukkeu.mukkeu.order.dto;

import java.util.List;

import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.order.domain.SourcePlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 결제 요청. 가게가 여러 곳이어도 요청은 한 번이다.
 *
 * 요청 한 번에 orders 행이 가게 수만큼 생기고 전부 같은 checkout_id 를 갖는다.
 * 나눠 보내면 한쪽만 성공했을 때 사용자는 결제했는데 한 집은 주문이 없는 상태가 된다.
 *
 * 전체 합계는 받지 않는다. 주문이 가게 단위로 쪼개져 저장되므로 넣어 둘 자리가 없다.
 * 장바구니 총액은 프론트가 subtotal 을 더해 그린다.
 *
 * 서버는 금액을 다시 계산하지 않는다. 해커톤 범위에서 내린 결정이고,
 * 실서비스라면 menu 를 다시 읽어 검증해야 한다.
 */
public record OrderCreateRequest(

	/**
	 * 출처 영상. **null 이 올 수 있다.**
	 *
	 * 족보 글에 영상이 안 붙어 있거나(글쓴이가 링크 없이 올린 경우) 예전 결제를
	 * "다시 주문" 할 때는 앱이 보낼 영상이 없다. order 의 source_* 컬럼은 전부
	 * nullable 이라 저장에는 문제가 없는데, 여기서만 필수로 막으면 그 두 흐름이
	 * 400 으로 죽는다.
	 */
	@Valid Source source,
	@Valid @NotEmpty List<Store> stores,

	/**
	 * 채움 포인트를 쓸지. **null 이면 false 다.**
	 *
	 * 필드를 새로 넣으면서도 기존 호출을 깨지 않으려고 Boolean 으로 둔다. 이 값을 안 보내는
	 * 클라이언트는 지금까지와 똑같이 동작한다 — 최소 주문 미달이면 400 이고, 잔액은 건드리지 않는다.
	 *
	 * true 면 서버가 가게별로 금액을 다시 계산한다. 다른 금액은 클라이언트 값을 그대로 믿지만
	 * (위 주석 참고) 포인트는 돈이라 여기만 예외로 둔다.
	 */
	Boolean usePrepaid
) {

	public boolean isUsePrepaid() {
		return Boolean.TRUE.equals(usePrepaid);
	}

	public record Source(
		@NotNull SourcePlatform platform,
		@NotBlank String url,
		String thumbnailUrl,
		String title
	) {
	}

	public record Store(
		@NotNull Long restaurantId,
		@NotBlank String restaurantName,
		@NotNull @PositiveOrZero Integer deliveryFee,
		@Valid @NotEmpty List<Item> items,
		@NotNull @PositiveOrZero Integer itemsTotal,
		@NotNull @PositiveOrZero Integer subtotal      // itemsTotal + deliveryFee
	) {
	}

	public record Item(
		@NotNull Long menuId,
		@NotBlank String menuName,
		@NotNull @PositiveOrZero Integer unitPrice,
		@NotNull @Positive Integer quantity,
		SpiceLevel selectedSpice,                      // 맵기 조절 불가 메뉴는 null

		/**
		 * 고른 것만 담는다. group 은 없으면 null 이고 키 자체를 빼지 않는다.
		 * menu.options 와 같은 모양이라 프론트가 변환할 것이 없다.
		 */
		List<MenuOption> selectedOptions,

		@NotNull @PositiveOrZero Integer optionsPrice,
		@NotNull @PositiveOrZero Integer lineTotal
	) {
	}
}
