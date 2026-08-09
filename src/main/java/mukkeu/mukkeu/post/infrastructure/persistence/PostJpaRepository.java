package mukkeu.mukkeu.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mukkeu.mukkeu.post.domain.Post;

public interface PostJpaRepository extends JpaRepository<Post, Long> {

	boolean existsByCheckoutId(Long checkoutId);

	/**
	 * 최신순. 커서는 post_id 다. IDENTITY 가 증가하므로 번호가 큰 것이 최신이고,
	 * 번호는 중복이 없어 같은 초에 두 글이 올라와도 하나가 건너뛰거나 두 번 나오지 않는다.
	 * 첫 페이지는 cursor 에 Long.MAX_VALUE 를 넣어 부른다.
	 */
	@Query(value = """
		SELECT * FROM post
		WHERE  post_id < :cursor
		ORDER  BY post_id DESC
		FETCH FIRST :size ROWS ONLY
		""", nativeQuery = true)
	List<Post> findLatest(@Param("cursor") Long cursor, @Param("size") int size);

	/**
	 * 인기순. 좋아요가 같은 글이 많아 post_id 만으로는 커서가 성립하지 않는다.
	 * (like_count, post_id) 복합으로 자른다 — 좋아요가 적거나, 같으면 번호가 작은 것.
	 * 첫 페이지는 (Integer.MAX_VALUE, Long.MAX_VALUE) 로 부른다.
	 */
	@Query(value = """
		SELECT * FROM post
		WHERE  (like_count < :likeCursor)
		   OR  (like_count = :likeCursor AND post_id < :idCursor)
		ORDER  BY like_count DESC, post_id DESC
		FETCH FIRST :size ROWS ONLY
		""", nativeQuery = true)
	List<Post> findPopular(@Param("likeCursor") int likeCursor,
		@Param("idCursor") Long idCursor, @Param("size") int size);

	List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	/**
	 * 카운터는 DB 에서 계산한다.
	 *
	 * 자바로 읽어서 더한 뒤 쓰면 두 요청이 같은 값을 읽고 같은 값을 써서 하나가 유실된다
	 * (lost update). 좋아요 100 개가 눌렸는데 97 로 남는 식이다.
	 * UPDATE ... = like_count + 1 은 DB 가 행 잠금을 잡고 계산해 이 문제가 없다.
	 */
	@Modifying
	@Query(value = "UPDATE post SET like_count = like_count + 1 WHERE post_id = :postId", nativeQuery = true)
	void increaseLikeCount(@Param("postId") Long postId);

	/** GREATEST 로 0 밑으로 내려가지 않게 막는다. 데이터가 어긋나도 음수는 보이면 안 된다. */
	@Modifying
	@Query(value = "UPDATE post SET like_count = GREATEST(like_count - 1, 0) WHERE post_id = :postId", nativeQuery = true)
	void decreaseLikeCount(@Param("postId") Long postId);

	@Modifying
	@Query(value = "UPDATE post SET comment_count = comment_count + 1 WHERE post_id = :postId", nativeQuery = true)
	void increaseCommentCount(@Param("postId") Long postId);

	@Modifying
	@Query(value = "UPDATE post SET comment_count = GREATEST(comment_count - 1, 0) WHERE post_id = :postId", nativeQuery = true)
	void decreaseCommentCount(@Param("postId") Long postId);

	/** 갱신 직후 화면에 내려줄 값. 영속성 컨텍스트를 거치지 않고 DB 를 다시 읽는다. */
	@Query(value = "SELECT like_count FROM post WHERE post_id = :postId", nativeQuery = true)
	Integer readLikeCount(@Param("postId") Long postId);
}
