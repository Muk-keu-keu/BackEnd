package mukkeu.mukkeu.post.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 댓글 목록. 오래된 순이다. 대화 흐름은 위에서 아래로 읽히는 것이 자연스럽다.
 * 커서를 두지 않는다. 한 글의 댓글이 수백 개가 되는 서비스가 아니다.
 */
public record CommentResponse(
	List<Comment> comments
) {

	public record Comment(
		Long commentId,
		Long authorId,
		String authorNickName,
		String body,
		OffsetDateTime createdAt,
		boolean mine              // 삭제 버튼을 그릴지
	) {
	}
}
