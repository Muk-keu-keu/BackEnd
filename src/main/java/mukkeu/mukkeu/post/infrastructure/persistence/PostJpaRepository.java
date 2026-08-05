package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.post.domain.Post;

public interface PostJpaRepository extends JpaRepository<Post, Long> {

	List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

	List<Post> findAllByOrderByLikeCountDescCreatedAtDesc(Pageable pageable);

	List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	boolean existsByCheckoutId(Long checkoutId);
}
