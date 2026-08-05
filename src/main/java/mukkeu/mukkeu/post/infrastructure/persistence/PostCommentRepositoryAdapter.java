package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mukkeu.mukkeu.post.domain.PostComment;
import mukkeu.mukkeu.post.domain.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostCommentRepositoryAdapter implements PostCommentRepository {

	private final PostCommentJpaRepository postCommentJpaRepository;

	@Override
	public PostComment save(PostComment comment) {
		return postCommentJpaRepository.save(comment);
	}

	@Override
	public Optional<PostComment> findById(Long id) {
		return postCommentJpaRepository.findById(id);
	}

	@Override
	public List<PostComment> findAllByPostId(Long postId) {
		return postCommentJpaRepository.findAllByPostIdOrderByCreatedAt(postId);
	}

	@Override
	public void delete(PostComment comment) {
		postCommentJpaRepository.delete(comment);
	}

	@Override
	@Transactional
	public void deleteAllByPostId(Long postId) {
		postCommentJpaRepository.deleteAllByPostId(postId);
	}
}
