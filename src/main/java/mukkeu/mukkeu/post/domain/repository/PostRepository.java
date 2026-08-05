package mukkeu.mukkeu.post.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.post.domain.Post;

public interface PostRepository {

	Post save(Post post);

	Optional<Post> findById(Long id);

	/** 요기족보 홈 — 최신순 */
	List<Post> findLatest(int size);

	/** 요기족보 홈 — 인기순. like_count 캐시를 그대로 정렬에 쓴다. */
	List<Post> findPopular(int size);

	/** 내가 쓴 글 */
	List<Post> findAllByUserId(Long userId, int size);

	/** 같은 결제로 두 번 쓰는 것을 막는다 (409) */
	boolean existsByCheckoutId(Long checkoutId);

	void delete(Post post);
}
