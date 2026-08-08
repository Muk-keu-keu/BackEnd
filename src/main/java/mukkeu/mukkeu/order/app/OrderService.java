package mukkeu.mukkeu.order.app;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.menu.app.OptionMatcher;
import mukkeu.mukkeu.menu.domain.Menu;
import mukkeu.mukkeu.menu.domain.MenuOption;
import mukkeu.mukkeu.menu.domain.repository.MenuRepository;
import mukkeu.mukkeu.order.domain.Order;
import mukkeu.mukkeu.order.domain.OrderItem;
import mukkeu.mukkeu.order.domain.repository.OrderItemRepository;
import mukkeu.mukkeu.order.domain.repository.OrderRepository;
import mukkeu.mukkeu.order.dto.OrderCreateRequest;
import mukkeu.mukkeu.order.dto.OrderCreateResponse;
import mukkeu.mukkeu.order.dto.OrderDetailResponse;
import mukkeu.mukkeu.order.dto.OrderListResponse;
import mukkeu.mukkeu.order.dto.SourceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 결제와 결제 내역.
 *
 * 행 1개 = 가게 1곳, checkout_id 1개 = 결제 1번이다. 배달이 가게 단위로 일어나므로
 * 행을 나누고, 사용자가 결제 버튼을 한 번 누른 사실은 checkout_id 로 남긴다.
 *
 * API 에는 checkoutId 만 나간다. order_id 는 내부 저장용이라 노출하지 않는다.
 * 내역도 상세도 족보 작성도 전부 결제 단위라 프론트가 가게별 행 번호를 알 이유가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;

	/** 커서 없이 첫 페이지를 부를 때 쓰는 값. SQL 에 NULL 분기를 두지 않기 위해서다. */
	private static final long NO_CURSOR = Long.MAX_VALUE;

	/** created_at 이 LocalDateTime 이라 지역 정보가 없다. 내보낼 때만 옷을 입힌다. */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final MenuRepository menuRepository;
	private final OptionMatcher optionMatcher;
	private final ObjectMapper objectMapper;

	// ────────────────────────────────────────────────────────
	//  결제
	// ────────────────────────────────────────────────────────

	/**
	 * 트랜잭션이 반드시 필요하다. 가게 3곳 중 2곳만 저장되면 사용자는 결제했는데
	 * 한 집은 주문이 없는 상태가 된다. 이 프로젝트에서 원자성이 진짜로 필요한 유일한 곳이다.
	 */
	@Transactional
	public OrderCreateResponse create(Long userId, OrderCreateRequest request) {

		// 결제 한 번에 한 번만 뽑는다. 반복문 안에서 부르면 가게마다 번호가 달라져 묶임이 깨진다.
		Long checkoutId = orderRepository.nextCheckoutId();

		// 메뉴 스냅샷용 조회. 가게마다 부르면 N+1 이라 결제 전체에서 한 번만 읽는다.
		Map<Long, Menu> menuById = loadMenus(request);

		List<Order> orders = request.stores().stream()
			.map(store -> toOrder(checkoutId, userId, request.source(), store))
			.toList();
		List<Order> saved = orderRepository.saveAll(orders);

		// saveAll 은 넘긴 순서를 지킨다. 그래서 i 번째 저장 결과가 i 번째 가게다.
		List<OrderItem> items = new ArrayList<>();
		for (int i = 0; i < saved.size(); i++) {
			Long orderId = saved.get(i).getId();
			for (OrderCreateRequest.Item item : request.stores().get(i).items()) {
				items.add(toOrderItem(orderId, item, menuById.get(item.menuId())));
			}
		}
		orderItemRepository.saveAll(items);

		return new OrderCreateResponse(
			request.stores().stream().map(OrderCreateRequest.Store::restaurantName).toList());
	}

	private Map<Long, Menu> loadMenus(OrderCreateRequest request) {
		List<Long> menuIds = request.stores().stream()
			.flatMap(s -> s.items().stream())
			.map(OrderCreateRequest.Item::menuId)
			.distinct()
			.toList();
		if (menuIds.isEmpty()) {
			return Map.of();
		}
		return menuRepository.findAllByIdIn(menuIds).stream()
			.collect(Collectors.toMap(Menu::getId, m -> m, (a, b) -> a));
	}

	private Order toOrder(Long checkoutId, Long userId,
		OrderCreateRequest.Source source, OrderCreateRequest.Store store) {

		return Order.builder()
			.checkoutId(checkoutId)
			.userId(userId)
			.restaurantId(store.restaurantId())
			.restaurantName(store.restaurantName())
			.deliveryFee(store.deliveryFee())
			.sourcePlatform(source.platform())
			.sourceUrl(source.url())
			.sourceThumbnail(source.thumbnailUrl())
			.sourceTitle(source.title())
			.itemsTotal(store.itemsTotal())
			.totalPrice(store.subtotal())
			.build();
	}

	/**
	 * 메뉴 설명·이미지는 요청에 없으므로 결제 시점의 menu 에서 가져와 박아 둔다.
	 * 나중에 메뉴가 바뀌거나 삭제돼도 지난 내역이 온전하다.
	 * 메뉴가 이미 사라졌으면 그냥 비워 둔다. 없는 메뉴 때문에 결제를 막을 이유가 없다.
	 */
	private OrderItem toOrderItem(Long orderId, OrderCreateRequest.Item item, Menu menu) {
		return OrderItem.builder()
			.orderId(orderId)
			.menuId(item.menuId())
			.menuName(item.menuName())
			.menuDesc(menu == null ? null : menu.getDescription())
			.menuImageUrl(menu == null ? null : menu.getImageUrl())
			.unitPrice(item.unitPrice())
			.quantity(item.quantity())
			.selectedSpice(item.selectedSpice())
			.selectedOptions(writeOptions(item.selectedOptions()))
			.optionsPrice(item.optionsPrice())
			.lineTotal(item.lineTotal())
			.build();
	}

	/** 고른 옵션을 JSON 문자열로. group 이 null 이어도 키를 남긴다. */
	private String writeOptions(List<MenuOption> options) {
		if (options == null || options.isEmpty()) {
			return null;
		}
		return objectMapper.writeValueAsString(options);
	}

	// ────────────────────────────────────────────────────────
	//  내역 목록
	// ────────────────────────────────────────────────────────

	/**
	 * 두 단계로 읽는다. 행 기준으로 잘라 버리면 한 결제가 반으로 나뉘어
	 * 카드에 가게가 하나만 뜬다. 그래서 카드 단위로 먼저 자르고 행을 채운다.
	 * 쿼리는 2번 고정이고 order_item 은 읽지 않는다.
	 */
	@Transactional(readOnly = true)
	public OrderListResponse getList(Long userId, String cursor, Integer size) {

		int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

		List<Long> checkoutIds = orderRepository.findCheckoutIdsByUserId(userId, parseCursor(cursor), limit);
		if (checkoutIds.isEmpty()) {
			return new OrderListResponse(List.of(), null);
		}

		// checkoutIds 가 최신순이므로 그 순서대로 카드를 만든다.
		Map<Long, List<Order>> byCheckout = new LinkedHashMap<>();
		checkoutIds.forEach(id -> byCheckout.put(id, new ArrayList<>()));
		orderRepository.findAllByCheckoutIdIn(checkoutIds)
			.forEach(o -> byCheckout.get(o.getCheckoutId()).add(o));

		List<OrderListResponse.Card> cards = byCheckout.values().stream()
			.filter(rows -> !rows.isEmpty())
			.map(rows -> new OrderListResponse.Card(
				rows.get(0).getCheckoutId(),
				toOffset(rows.get(0).getCreatedAt()),
				toSource(rows.get(0)),
				rows.stream().map(Order::getRestaurantName).toList(),
				rows.stream().mapToInt(Order::getTotalPrice).sum()))
			.toList();

		// 받은 수가 요청보다 적으면 마지막 페이지다.
		String nextCursor = checkoutIds.size() < limit
			? null
			: String.valueOf(checkoutIds.get(checkoutIds.size() - 1));

		return new OrderListResponse(cards, nextCursor);
	}

	/** 커서는 불투명한 문자열로 주고받되 내부적으로는 checkout_id 다. */
	private Long parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return NO_CURSOR;
		}
		try {
			return Long.valueOf(cursor.trim());
		} catch (NumberFormatException e) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	// ────────────────────────────────────────────────────────
	//  내역 상세
	// ────────────────────────────────────────────────────────

	/**
	 * 쿼리에 user_id 조건이 들어 있어 남의 결제 번호를 넣어도 0행이다.
	 * 없는 번호든 남의 번호든 똑같이 404 라서 "그 번호가 존재한다" 는 사실이 새지 않는다.
	 */
	@Transactional(readOnly = true)
	public OrderDetailResponse getDetail(Long userId, Long checkoutId) {

		List<Order> orders = orderRepository.findAllByCheckoutIdAndUserId(checkoutId, userId);
		if (orders.isEmpty()) {
			throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
		}

		Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository
			.findAllByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
			.collect(Collectors.groupingBy(OrderItem::getOrderId));

		List<OrderDetailResponse.Store> stores = orders.stream()
			.map(o -> new OrderDetailResponse.Store(
				o.getRestaurantId(), o.getRestaurantName(), o.getDeliveryFee(),
				itemsByOrder.getOrDefault(o.getId(), List.of()).stream().map(this::toItem).toList(),
				o.getItemsTotal(), o.getTotalPrice()))
			.toList();

		return new OrderDetailResponse(
			checkoutId,
			toOffset(orders.get(0).getCreatedAt()),
			toSource(orders.get(0)),
			stores,
			orders.stream().mapToInt(Order::getTotalPrice).sum());
	}

	private OrderDetailResponse.Item toItem(OrderItem item) {
		return new OrderDetailResponse.Item(
			item.getMenuId(), item.getMenuName(), item.getMenuImageUrl(),
			item.getUnitPrice(), item.getQuantity(), item.getSelectedSpice(),
			optionMatcher.parse(item.getSelectedOptions()),
			item.getOptionsPrice(), item.getLineTotal());
	}

	// ────────────────────────────────────────────────────────
	//  조립
	// ────────────────────────────────────────────────────────

	private SourceResponse toSource(Order order) {
		return new SourceResponse(
			order.getSourcePlatform(), order.getSourceUrl(),
			order.getSourceThumbnail(), order.getSourceTitle());
	}

	/**
	 * LocalDateTime 은 지역 정보가 없어 "2026-08-04T19:22:10" 으로만 나간다.
	 * 프론트가 자기 로컬 시간으로 해석하면 서버 존과 다를 때 시각이 어긋난다.
	 * +09:00 을 붙여 문자열 자체가 순간을 확정하게 만든다.
	 */
	private OffsetDateTime toOffset(LocalDateTime time) {
		return time == null ? null : time.atZone(KST).toOffsetDateTime();
	}
}
