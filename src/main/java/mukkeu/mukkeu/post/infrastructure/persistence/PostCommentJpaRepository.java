package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.post.domain.PostComment;

public interface PostCommentJpaRepository extends JpaRepository<PostComment, Long> {

	List<PostComment> findAllByPostIdOrderByCreatedAt(Long postId);

	void deleteAllByPostId(Long postId);
}
