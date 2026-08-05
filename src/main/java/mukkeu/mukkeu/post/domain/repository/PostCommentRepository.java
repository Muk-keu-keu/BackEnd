package mukkeu.mukkeu.post.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.post.domain.PostComment;

public interface PostCommentRepository {

	PostComment save(PostComment comment);

	Optional<PostComment> findById(Long id);

	List<PostComment> findAllByPostId(Long postId);

	void delete(PostComment comment);

	void deleteAllByPostId(Long postId);
}
