package mukkeu.mukkeu.menu.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.menu.app.MenuBoardService;
import mukkeu.mukkeu.menu.dto.MenuBoardResponse;
import mukkeu.mukkeu.user.app.UserContextService;
import lombok.RequiredArgsConstructor;

/**
 * 가게 메뉴판. 먹방요기 결과에서 "메뉴 수정" 을 눌렀을 때 선택지를 채운다.
 *
 * 거리와 도착예정을 사용자 좌표로 계산하므로 로그인이 필요하다. 좌표를 쿼리
 * 파라미터로 받지 않는 이유는 먹방요기와 같다 — 배달 주소는 계정에 붙은 값이고,
 * 클라이언트가 보내게 하면 화면마다 다른 좌표가 섞인다.
 */
@RestController
@RequestMapping("/v1/restaurants/{restaurantId}/menus")
@RequiredArgsConstructor
public class MenuBoardController {

	private final MenuBoardService menuBoardService;
	private final UserContextService userContextService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public MenuBoardResponse getMenus(@PathVariable Long restaurantId) {
		return menuBoardService.getBoard(userContextService.getCurrentUserIdOrNull(), restaurantId);
	}
}
