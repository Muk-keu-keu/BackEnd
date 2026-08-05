package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.post.domain.PostImage;
import mukkeu.mukkeu.post.domain.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostImageRepositoryAdapter implements PostImageRepository {

	private final PostImageJpaRepository postImageJpaRepository;

	@Override
	public List<PostImage> saveAll(List<PostImage> images) {
		return postImageJpaRepository.saveAll(images);
	}

	@Override
	public List<PostImage> findAllByPostId(Long postId) {
		return postImageJpaRepository.findAllByPostIdOrderBySortOrder(postId);
	}

	@Override
	public List<PostImage> findAllByPostIdIn(List<Long> postIds) {
		return postImageJpaRepository.findAllByPostIdInOrderBySortOrder(postIds);
	}

	@Override
	public void deleteAllByPostId(Long postId) {
		postImageJpaRepository.deleteAllByPostId(postId);
	}
}
