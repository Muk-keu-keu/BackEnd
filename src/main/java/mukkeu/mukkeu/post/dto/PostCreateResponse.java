package mukkeu.mukkeu.post.dto;

/**
 * 201 응답. 방금 만든 글 번호만 준다.
 * 프론트가 작성 직후 상세로 보내는 흐름이라 이 값 하나면 된다.
 */
public record PostCreateResponse(Long postId) {
}
