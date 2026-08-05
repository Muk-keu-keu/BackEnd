package mukkeu.mukkeu.post.domain;

import java.io.Serializable;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** post_like 의 복합 기본키 (post_id, user_id) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikeId implements Serializable {

	private Long postId;
	private Long userId;

	public PostLikeId(Long postId, Long userId) {
		this.postId = postId;
		this.userId = userId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PostLikeId other)) {
			return false;
		}
		return Objects.equals(postId, other.postId) && Objects.equals(userId, other.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(postId, userId);
	}
}
