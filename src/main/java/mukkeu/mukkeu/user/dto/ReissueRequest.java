package mukkeu.mukkeu.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 앱이 저장하고 있던 refresh token으로 재발급을 요청한다.
 */
public record ReissueRequest(

	@NotBlank(message = "refreshToken은 필수입니다.") String refreshToken
) {
}
