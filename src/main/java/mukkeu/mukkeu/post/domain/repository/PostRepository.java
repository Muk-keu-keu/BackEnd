package mukkeu.mukkeu.post.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.post.domain.Post;

public interface PostRepository {

	Post save(Post post);

	Optional<Post> findById(Long id);

	/**
	 * 요기족보 홈 — 최신순. 커서는 postId 다.
	 * 첫 페이지는 cursor 에 null 을 넘긴다.
	 */
	List<Post> findLatest(Long cursor, int size);

	/**
	 * 요기족보 홈 — 인기순. like_count 캐시를 그대로 정렬에 쓴다.
	 * 좋아요가 같은 글이 많아 커서가 (likeCount, postId) 복합이다.
	 * 첫 페이지는 둘 다 null 을 넘긴다.
	 */
	List<Post> findPopular(Integer likeCursor, Long idCursor, int size);

	/** 내가 쓴 글 */
	List<Post> findAllByUserId(Long userId, int size);

	/** 같은 결제로 두 번 쓰는 것을 막는다 (409) */
	boolean existsByCheckoutId(Long checkoutId);

	void delete(Post post);

	// ── 카운터. 자바에서 읽고 더하면 동시 요청에 유실된다 ──
	void increaseLikeCount(Long postId);

	void decreaseLikeCount(Long postId);

	void increaseCommentCount(Long postId);

	void decreaseCommentCount(Long postId);

	/** 갱신 직후 화면에 내려줄 값 */
	int readLikeCount(Long postId);
}
