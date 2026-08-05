package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.post.domain.PostImage;

public interface PostImageJpaRepository extends JpaRepository<PostImage, Long> {

	List<PostImage> findAllByPostIdOrderBySortOrder(Long postId);

	List<PostImage> findAllByPostIdInOrderBySortOrder(List<Long> postIds);

	void deleteAllByPostId(Long postId);
}
