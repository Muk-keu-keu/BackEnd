package mukkeu.mukkeu.credit.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.credit.app.CreditQueryService;
import mukkeu.mukkeu.credit.dto.CreditBalanceResponse;
import mukkeu.mukkeu.user.app.UserContextService;
import lombok.RequiredArgsConstructor;

/**
 * 채움 포인트 조회.
 *
 * 적립·사용은 여기에 없다. 포인트는 결제의 부산물이라 결제(POST /v1/orders)에서만 움직인다.
 * 포인트만 따로 충전하거나 쓰는 길을 열면 그 순간 선불충전 서비스가 된다.
 */
@RestController
@RequestMapping("/v1/credits")
@RequiredArgsConstructor
public class CreditController {

	private final CreditQueryService creditQueryService;
	private final UserContextService userContextService;

	/** 잔액이 남은 가게만 잔액 많은 순으로. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public CreditBalanceResponse getMyCredits() {
		return creditQueryService.getMyCredits(userContextService.getCurrentUserId());
	}

	/** 가게 한 곳. 거래가 없었으면 balance 가 0 이다. 404 를 주지 않는다. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}")
	public CreditBalanceResponse.StoreCredit getOne(@PathVariable Long restaurantId) {
		return creditQueryService.getOne(userContextService.getCurrentUserId(), restaurantId);
	}
}
