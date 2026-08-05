package mukkeu.mukkeu.post.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좋아요. 한 사람이 한 글에 한 번만 — 복합 PK 가 중복을 막는다.
 * 실제 개수는 post.like_count 캐시를 쓰고, 이 테이블은 "내가 눌렀나" 판정용이다.
 */
@Entity
@Getter
@Table(name = "post_like")
@IdClass(PostLikeId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "liked_at", nullable = false)
	private LocalDateTime likedAt;

	public PostLike(Long postId, Long userId) {
		this.postId = postId;
		this.userId = userId;
		this.likedAt = LocalDateTime.now();
	}
}
