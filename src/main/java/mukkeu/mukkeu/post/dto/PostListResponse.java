package mukkeu.mukkeu.post.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 요기족보 홈. 카드 하나 = 글 하나.
 *
 * 비로그인도 볼 수 있다. 토큰이 없으면 liked 가 전부 false 로 내려간다.
 *
 * nextCursor 는 정렬 기준에 따라 모양이 다르다.
 *   LATEST   "9012"          postId
 *   POPULAR  "37:9012"       likeCount:postId — 좋아요가 같은 글이 많아 복합이어야 한다
 * 프론트는 해석하지 말고 그대로 되돌려주면 된다.
 */
public record PostListResponse(
	List<Card> posts,
	String nextCursor
) {

	public record Card(
		Long postId,
		String title,
		String thumbnailUrl,      // 첫 번째 이미지. 없으면 null
		String authorNickName,
		OffsetDateTime createdAt,
		int likeCount,
		int commentCount,
		boolean liked             // 비로그인이면 항상 false
	) {
	}
}
