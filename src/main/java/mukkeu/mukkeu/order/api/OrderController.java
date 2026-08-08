package mukkeu.mukkeu.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.order.app.OrderService;
import mukkeu.mukkeu.order.dto.OrderCreateRequest;
import mukkeu.mukkeu.order.dto.OrderCreateResponse;
import mukkeu.mukkeu.order.dto.OrderDetailResponse;
import mukkeu.mukkeu.order.dto.OrderListResponse;
import mukkeu.mukkeu.user.app.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 결제와 결제 내역. 경로의 식별자는 전부 checkoutId 다.
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final UserContextService userContextService;

	/** 가게가 여러 곳이어도 요청은 한 번이다. 부분 실패가 없도록 한 트랜잭션으로 처리한다. */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public OrderCreateResponse create(@RequestBody @Valid OrderCreateRequest request) {
		return orderService.create(userContextService.getCurrentUserId(), request);
	}

	/** 무한 스크롤. 다음 페이지는 응답의 nextCursor 를 그대로 cursor 에 넣어 부른다. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public OrderListResponse getList(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false) Integer size) {

		return orderService.getList(userContextService.getCurrentUserId(), cursor, size);
	}

	/** 남의 결제 번호를 넣어도 404 다. 403 을 주면 그 번호의 존재가 드러난다. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{checkoutId}")
	public OrderDetailResponse getDetail(@PathVariable Long checkoutId) {
		return orderService.getDetail(userContextService.getCurrentUserId(), checkoutId);
	}
}
