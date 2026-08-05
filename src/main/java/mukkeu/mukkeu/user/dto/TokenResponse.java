package mukkeu.mukkeu.user.dto;

/**
 * 앱 클라이언트가 두 토큰을 모두 저장할 수 있도록 body로 내려주는 응답
 */
public record TokenResponse(
	String accessToken,
	String refreshToken
) {
}
