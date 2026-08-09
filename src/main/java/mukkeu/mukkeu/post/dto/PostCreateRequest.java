package mukkeu.mukkeu.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 요기족보 작성. multipart 의 평범한 텍스트 필드로 온다(checkoutId / title / body).
 * JSON 파트가 아니다 — 파트별 Content-Type 을 클라이언트가 못 붙이는 경우가 많아서다.
 *
 * 이미지는 같은 요청의 images 파트로 함께 온다. 업로드를 따로 떼면 사용자가 글을
 * 안 쓰고 이탈했을 때 주인 없는 파일이 버킷에 남는다.
 *
 * 글은 결제 단위다. 영상 속 조합이 여러 가게에 걸치므로 가게가 아니라 checkoutId 를 받는다.
 */
public record PostCreateRequest(

	@NotNull Long checkoutId,

	@NotBlank @Size(max = 20) String title,

	@NotBlank @Size(max = 400) String body
) {
}
