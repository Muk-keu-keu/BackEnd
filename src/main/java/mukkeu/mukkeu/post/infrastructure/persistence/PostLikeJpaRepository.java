package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mukkeu.mukkeu.post.domain.PostLike;
import mukkeu.mukkeu.post.domain.PostLikeId;

public interface PostLikeJpaRepository extends JpaRepository<PostLike, PostLikeId> {

	boolean existsByPostIdAndUserId(Long postId, Long userId);

	@Query("SELECT l.postId FROM PostLike l WHERE l.userId = :userId AND l.postId IN :postIds")
	List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

	void deleteByPostIdAndUserId(Long postId, Long userId);

	void deleteAllByPostId(Long postId);
}
