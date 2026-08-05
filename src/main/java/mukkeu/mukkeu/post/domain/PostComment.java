package mukkeu.mukkeu.post.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** created_at / updated_at 은 BaseEntity 의 Auditing 이 채운다. */
@Entity
@Getter
@Table(name = "post_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false))
public class PostComment extends BaseEntity {

	@Id
	@Column(name = "comment_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 500)
	private String body;

	@Builder
	private PostComment(Long postId, Long userId, String body) {
		this.postId = postId;
		this.userId = userId;
		this.body = body;
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}
}
