package mukkeu.mukkeu.user.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mukkeu.mukkeu.user.app.UserContextService;
import mukkeu.mukkeu.user.app.UserService;
import mukkeu.mukkeu.user.dto.DeleteUserRequest;
import mukkeu.mukkeu.user.dto.LoginRequest;
import mukkeu.mukkeu.user.dto.ReissueRequest;
import mukkeu.mukkeu.user.dto.SignUpRequest;
import mukkeu.mukkeu.user.dto.TokenResponse;
import mukkeu.mukkeu.user.dto.UpdateNickNameRequest;
import mukkeu.mukkeu.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 응답은 껍데기 없이 순수 body 만 내려준다.
 * 상태는 HTTP status code 로만 표현한다.
 */
@RestController
@RequestMapping("v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserContextService userContextService;

	/** 회원가입 : 이메일 + 비밀번호 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/signup")
	public void signup(@RequestBody @Valid SignUpRequest request) {
		userService.signup(request);
	}

	/** 로그인 : 앱이 저장할 수 있도록 access / refresh 토큰을 body로 함께 내려준다 */
	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/login")
	public TokenResponse login(@RequestBody @Valid LoginRequest request) {
		return userService.login(request);
	}

	/** 로그아웃 : 서버의 refresh token 삭제 (앱에서도 저장한 토큰을 지워야 한다) */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/logout")
	public void logout() {
		userService.logout(userContextService.getCurrentUserId());
	}

	/** access token 만료 시 앱이 보관하던 refresh token으로 재발급 */
	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/reissue-token")
	public TokenResponse reissueToken(@RequestBody @Valid ReissueRequest request) {
		return userService.reissue(request.refreshToken());
	}

	/** 내 정보 조회 (토큰이 잘 붙는지 확인용) */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/me")
	public UserResponse me() {
		return userService.getMyInfo(userContextService.getCurrentUserId());
	}

	/** 닉네임 수정 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping("/me")
	public void updateNickName(@RequestBody @Valid UpdateNickNameRequest request) {
		userService.updateNickName(userContextService.getCurrentUserId(), request);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/delete")
	public void deleteUser(@RequestBody @Valid DeleteUserRequest request) {
		userService.deleteUser(userContextService.getCurrentUserId(), request);
	}
}
