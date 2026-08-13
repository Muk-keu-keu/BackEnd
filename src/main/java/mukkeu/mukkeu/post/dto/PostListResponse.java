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

		/**
		 * 본문 전체. 카드가 두 줄만 보여주더라도 자르지 않고 그대로 보낸다.
		 *
		 * 서버가 잘라 보내면 프론트가 "이게 전부인지 잘린 것인지" 를 알 수 없고,
		 * 상세 API 의 body 와 값이 달라져 같은 글이 화면마다 다르게 보인다.
		 * 최대 400자라 20개를 실어도 8KB 수준이다. 몇 줄을 보여줄지는 화면이 정한다.
		 */
		String body,

		String thumbnailUrl,      // 첫 번째 이미지. 없으면 null
		String authorNickName,
		OffsetDateTime createdAt,
		int likeCount,
		int commentCount,
		boolean liked             // 비로그인이면 항상 false
	) {
	}
}
