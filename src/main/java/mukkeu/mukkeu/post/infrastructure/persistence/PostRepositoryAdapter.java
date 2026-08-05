package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.post.domain.Post;
import mukkeu.mukkeu.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostRepositoryAdapter implements PostRepository {

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
	public List<Post> findLatest(int size) {
		return postJpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size));
	}

	@Override
	public List<Post> findPopular(int size) {
		return postJpaRepository.findAllByOrderByLikeCountDescCreatedAtDesc(PageRequest.of(0, size));
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
}
