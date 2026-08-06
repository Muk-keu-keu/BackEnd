package mukkeu.mukkeu.user.app;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.user.adapter.UserDetail;
import mukkeu.mukkeu.user.domain.RefreshToken;
import mukkeu.mukkeu.user.domain.User;
import mukkeu.mukkeu.user.domain.repository.RefreshTokenRepository;
import mukkeu.mukkeu.user.domain.repository.UserRepository;
import mukkeu.mukkeu.user.dto.DeleteUserRequest;
import mukkeu.mukkeu.user.dto.LoginRequest;
import mukkeu.mukkeu.user.dto.SignUpRequest;
import mukkeu.mukkeu.user.dto.TokenResponse;
import mukkeu.mukkeu.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * 회원가입 : 이메일 + 비밀번호 (비밀번호는 암호화해서 저장)
	 */
	@Transactional
	public void signup(SignUpRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);   // 409
		}
		String encodePassword = passwordEncoder.encode(request.password());
		// 동시 요청으로 여기를 같이 통과하더라도 users.email unique 제약이 마지막으로 막아준다
		userRepository.save(User.create(request.email(), encodePassword, request.nickName()));
	}

	@Transactional(readOnly = true)
	public User getById(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));   // 404
	}

	@Transactional(readOnly = true)
	public UserResponse getMyInfo(Long id) {
		return UserResponse.from(getById(id));
	}

	@Transactional(readOnly = true)
	public UserDetail getDetails(Long id) {
		return UserDetail.from(getById(id));
	}

	/**
	 * 로그인 : access / refresh 토큰을 모두 응답 body로 내려준다. (앱 클라이언트)
	 */
	@Transactional
	public TokenResponse login(LoginRequest request) {
		// 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 응답으로 처리해 가입 여부가 노출되지 않게 한다
		User user = userRepository.findByEmail(request.email())
			.filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));   // 401

		String refreshToken = issueRefreshTokenOnLogin(user);
		String accessToken = jwtTokenProvider.issueAccessToken(user.getId(), user.getRole(), user.getEmail());

		return toTokenResponse(accessToken, refreshToken);
	}

	/**
	 * 로그인할 때마다 refresh token을 새로 발급해 이전 토큰을 무효화한다.
	 * 살아 있는 토큰을 재사용하면 로그아웃이 실패했을 때 이전 토큰이 만료일까지 그대로 유효해진다.
	 */
	private String issueRefreshTokenOnLogin(User user) {
		String issued = jwtTokenProvider.issueRefreshToken(user.getId(), user.getRole(), user.getEmail());

		refreshTokenRepository.findLatestByUserId(user.getId())
			.ifPresentOrElse(
				// 기존 행을 덮어쓴다 (더티 체킹)
				token -> token.newSetRefreshToken(issued),
				() -> refreshTokenRepository.save(new RefreshToken(issued, user.getId())));

		return issued;
	}

	/**
	 * access token 만료 시 앱이 보관하던 refresh token으로 재발급.
	 * refresh token도 노출 가능성이 있으므로 사용한 뒤 함께 갱신한다.
	 */
	@Transactional
	public TokenResponse reissue(String refreshToken) {
		if (refreshToken == null || !jwtTokenProvider.validate(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);   // 401
		}

		Long userId = jwtTokenProvider.parseJwt(refreshToken).userId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));   // 404

		// 서버가 발급한 토큰이 맞는지 대조 (위조 / 탈취 후 재사용 방지)
		RefreshToken savedToken = refreshTokenRepository.findLatestByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));   // 401

		if (!savedToken.getRefreshToken().equals(refreshToken)) {
			log.warn("저장된 refresh token과 불일치 (위조 가능성) userId={}", userId);
			throw new BusinessException(ErrorCode.TOKEN_MISMATCH);   // 401
		}

		String newAccessToken = jwtTokenProvider.issueAccessToken(user.getId(), user.getRole(), user.getEmail());
		String newRefreshToken = jwtTokenProvider.issueRefreshToken(user.getId(), user.getRole(), user.getEmail());
		savedToken.newSetRefreshToken(newRefreshToken);

		return toTokenResponse(newAccessToken, newRefreshToken);
	}

	/**
	 * 로그아웃 : 서버에 저장된 refresh token을 지운다.
	 * (Redis를 쓰지 않으므로 이미 발급된 access token은 만료 시간까지는 유효하다 → 앱에서도 삭제할 것)
	 */
	@Transactional
	public void logout(Long userId) {
		refreshTokenRepository.deleteAllByUserId(userId);
	}

	/**
	 * 회원 탈퇴 : 토큰 주인 + 입력한 이메일/비밀번호가 모두 일치해야 한다.
	 */
	@Transactional
	public void deleteUser(Long userId, DeleteUserRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));   // 404

		if (!request.email().equals(user.getEmail())
			|| !passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);   // 401
		}

		refreshTokenRepository.deleteAllByUserId(userId);
		userRepository.delete(user);
	}

	private TokenResponse toTokenResponse(String accessToken, String refreshToken) {
		return new TokenResponse(accessToken, refreshToken);
	}
}
