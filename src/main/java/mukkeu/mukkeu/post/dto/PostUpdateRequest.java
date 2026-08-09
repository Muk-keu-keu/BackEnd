package mukkeu.mukkeu.post.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 요기족보 수정. 작성과 같이 multipart 의 텍스트 필드로 온다(title / body / keepImageUrls).
 * keepImageUrls 는 같은 이름의 필드를 여러 줄 보내면 순서대로 리스트가 된다.
 *
 * 이미지 편집을 keepImageUrls 하나로 처리한다.
 *   남길 URL 을 원하는 순서로 담으면 → 그 순서가 새 sort_order 가 된다 (재배열)
 *   목록에서 뺀 URL 은                → 삭제된다
 *   images 파트에 파일을 더 넣으면     → keepImageUrls 뒤에 붙는다 (추가)
 *
 * 삭제/재배열/추가를 각각 API 로 나누면 프론트가 세 번 호출하며 중간 상태를 관리해야 한다.
 * 최종 상태를 통째로 받으면 서버가 그대로 맞추면 된다.
 *
 * keepImageUrls 를 아예 보내지 않으면 기존 이미지를 전부 지운다는 뜻이다.
 * 실수로 비면 사진이 날아가므로 프론트가 항상 명시해서 보내는 편이 안전하다.
 *
 * checkoutId 는 받지 않는다. 어느 결제를 자랑하는 글인지는 바꿀 수 없다.
 */
public record PostUpdateRequest(

	@NotBlank @Size(max = 20) String title,

	@NotBlank @Size(max = 400) String body,

	List<String> keepImageUrls
) {
}
