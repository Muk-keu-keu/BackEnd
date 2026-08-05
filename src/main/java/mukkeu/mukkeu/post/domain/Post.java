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

/**
 * 요기족보 게시글 — 내가 실제로 시킨 조합을 공개 공유한다.
 *
 * 글 하나의 단위는 결제 한 번(checkout_id)이다. 엽떡 + 교촌을 한 번에
 * 결제했으면 글 하나에 두 가게가 다 들어간다. 이 서비스가 파는 것이
 * "영상 속 조합" 이고 조합은 브랜드를 넘나들기 때문이다.
 *
 * ★ checkout_id 에 FK 를 걸 수 없다. orders 안에서 유일한 값이 아니라
 *   참조 대상이 될 수 없다. "그 결제가 내 것으로 존재하나" 는 서비스가
 *   OrderRepository.existsByCheckoutIdAndUserId 로 직접 검사한다.
 *
 * ★ 조합 내용은 복사하지 않는다. order_item 이 이미 결제 시점 스냅샷이라
 *   또 복사하면 같은 것을 두 군데 두는 셈이다.
 *
 * ★ created_at / updated_at 은 BaseEntity 의 Auditing 이 채운다.
 *   생성자에서 직접 넣지 않는다 — flush 시점에 들어간다.
 */
@Entity
@Getter
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false))
public class Post extends BaseEntity {

	@Id
	@Column(name = "post_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** orders.checkout_id. UNIQUE 라 한 결제로 글을 두 번 못 쓴다. */
	@Column(name = "checkout_id", nullable = false, unique = true)
	private Long checkoutId;

	@Column(nullable = false, length = 20)
	private String title;

	@Column(nullable = false, length = 400)
	private String body;

	/** 비정규화 캐시. 목록에서 글마다 COUNT 를 돌리면 느려진다. */
	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "comment_count", nullable = false)
	private Integer commentCount;

	@Builder
	private Post(Long userId, Long checkoutId, String title, String body) {
		this.userId = userId;
		this.checkoutId = checkoutId;
		this.title = title;
		this.body = body;
		this.likeCount = 0;
		this.commentCount = 0;
	}

	public void increaseLikeCount() {
		this.likeCount++;
	}

	public void decreaseLikeCount() {
		if (this.likeCount > 0) {
			this.likeCount--;
		}
	}

	public void increaseCommentCount() {
		this.commentCount++;
	}

	public void decreaseCommentCount() {
		if (this.commentCount > 0) {
			this.commentCount--;
		}
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}
}
