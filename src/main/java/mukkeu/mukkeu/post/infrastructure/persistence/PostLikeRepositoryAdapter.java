package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.post.domain.PostLike;
import mukkeu.mukkeu.post.domain.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostLikeRepositoryAdapter implements PostLikeRepository {

	private final PostLikeJpaRepository postLikeJpaRepository;

	@Override
	public PostLike save(PostLike like) {
		return postLikeJpaRepository.save(like);
	}

	@Override
	public boolean exists(Long postId, Long userId) {
		return postLikeJpaRepository.existsByPostIdAndUserId(postId, userId);
	}

	@Override
	public List<Long> findLikedPostIds(Long userId, List<Long> postIds) {
		if (postIds == null || postIds.isEmpty()) {
			return List.of();
		}
		return postLikeJpaRepository.findLikedPostIds(userId, postIds);
	}

	@Override
	@Transactional
	public void delete(Long postId, Long userId) {
		postLikeJpaRepository.deleteByPostIdAndUserId(postId, userId);
	}

	@Override
	@Transactional
	public void deleteAllByPostId(Long postId) {
		postLikeJpaRepository.deleteAllByPostId(postId);
	}
}
