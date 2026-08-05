package mukkeu.mukkeu.post.domain.repository;

import java.util.List;

import mukkeu.mukkeu.post.domain.PostImage;

public interface PostImageRepository {

	List<PostImage> saveAll(List<PostImage> images);

	List<PostImage> findAllByPostId(Long postId);

	List<PostImage> findAllByPostIdIn(List<Long> postIds);

	void deleteAllByPostId(Long postId);
}
