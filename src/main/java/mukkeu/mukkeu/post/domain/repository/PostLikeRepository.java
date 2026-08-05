package mukkeu.mukkeu.post.domain.repository;

import java.util.List;

import mukkeu.mukkeu.post.domain.PostLike;

public interface PostLikeRepository {

	PostLike save(PostLike like);

	boolean exists(Long postId, Long userId);

	/** 목록에서 "내가 누른 글" 을 한 번에 판정한다. 글마다 조회하면 N+1 이 된다. */
	List<Long> findLikedPostIds(Long userId, List<Long> postIds);

	void delete(Long postId, Long userId);

	void deleteAllByPostId(Long postId);
}
