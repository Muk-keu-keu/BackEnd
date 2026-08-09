package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.post.domain.Post;
import mukkeu.mukkeu.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

	/** 커서 없이 첫 페이지를 부를 때 쓰는 값. SQL 에 NULL 분기를 두지 않기 위해서다. */
	private static final long NO_ID_CURSOR = Long.MAX_VALUE;
	private static final int NO_LIKE_CURSOR = Integer.MAX_VALUE;

	private final PostJpaRepository postJpaRepository;

	@Override
	public Post save(Post post) {
		return postJpaRepository.save(post);
	}

	@Override
	public Optional<Post> findById(Long id) {
		return postJpaRepository.findById(id);
	}

	@Override
	public List<Post> findLatest(Long cursor, int size) {
		return postJpaRepository.findLatest(cursor == null ? NO_ID_CURSOR : cursor, size);
	}

	@Override
	public List<Post> findPopular(Integer likeCursor, Long idCursor, int size) {
		return postJpaRepository.findPopular(
			likeCursor == null ? NO_LIKE_CURSOR : likeCursor,
			idCursor == null ? NO_ID_CURSOR : idCursor,
			size);
	}

	@Override
	public List<Post> findAllByUserId(Long userId, int size) {
		return postJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size));
	}

	@Override
	public boolean existsByCheckoutId(Long checkoutId) {
		return postJpaRepository.existsByCheckoutId(checkoutId);
	}

	@Override
	public void delete(Post post) {
		postJpaRepository.delete(post);
	}

	// ── 카운터 ──
	// @Modifying 쿼리는 트랜잭션 안에서만 돌 수 있다. 호출하는 서비스에도
	// @Transactional 이 있지만, 어댑터만 따로 쓰이는 경우를 대비해 여기에도 건다.

	@Override
	@Transactional
	public void increaseLikeCount(Long postId) {
		postJpaRepository.increaseLikeCount(postId);
	}

	@Override
	@Transactional
	public void decreaseLikeCount(Long postId) {
		postJpaRepository.decreaseLikeCount(postId);
	}

	@Override
	@Transactional
	public void increaseCommentCount(Long postId) {
		postJpaRepository.increaseCommentCount(postId);
	}

	@Override
	@Transactional
	public void decreaseCommentCount(Long postId) {
		postJpaRepository.decreaseCommentCount(postId);
	}

	@Override
	public int readLikeCount(Long postId) {
		Integer count = postJpaRepository.readLikeCount(postId);
		return count == null ? 0 : count;
	}
}
