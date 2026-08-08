package mukkeu.mukkeu.menu.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.menu.app.MenuBoardService;
import mukkeu.mukkeu.menu.dto.MenuBoardResponse;
import lombok.RequiredArgsConstructor;

/**
 * 가게 메뉴판. 먹방요기 결과에서 "메뉴 수정" 을 눌렀을 때 선택지를 채운다.
 */
@RestController
@RequestMapping("/v1/restaurants/{restaurantId}/menus")
@RequiredArgsConstructor
public class MenuBoardController {

	private final MenuBoardService menuBoardService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public MenuBoardResponse getMenus(@PathVariable Long restaurantId) {
		return menuBoardService.getBoard(restaurantId);
	}
}
