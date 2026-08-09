package mukkeu.mukkeu.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 요기족보 수정. 작성과 완전히 같은 모양이다 (checkoutId 만 없다).
 * multipart 의 평범한 텍스트 필드로 온다.
 *
 * 이미지는 keepImageUrls 같은 목록을 받지 않는다. 수정 후 남을 사진 전부를
 * images 파트에 파일로 다시 보낸다. 기존 사진도 예외가 아니다.
 *
 * 프론트가 편집기를 열 때 기존 사진을 File 로 만들어 두면, 그 뒤로는 파일 배열
 * 하나만 다루면 된다. 순서 바꾸기·중간에 끼우기·빼기가 전부 배열 조작으로 끝나고
 * 작성 화면과 같은 컴포넌트를 쓸 수 있다.
 *
 * URL 목록으로 받던 방식은 새 파일이 아직 URL 이 없어 항상 뒤에만 붙었고,
 * "목록을 안 보내면 사진이 전부 지워진다" 는 함정이 있었다.
 *
 * checkoutId 는 받지 않는다. 어느 결제를 자랑하는 글인지는 바꿀 수 없다.
 */
public record PostUpdateRequest(

	@NotBlank @Size(max = 20) String title,

	@NotBlank @Size(max = 400) String body
) {
}
