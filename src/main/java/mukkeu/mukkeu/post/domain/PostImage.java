package mukkeu.mukkeu.post.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
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
 * 사용자가 직접 올린 사진. 영상 썸네일과는 다르다 —
 * 썸네일은 orders.source_thumbnail 에 있고 목록 카드에는 그쪽을 쓴다.
 *
 * 업로드 API 를 따로 두지 않는다. 글 작성(multipart)에서 같이 받아
 * 한 트랜잭션으로 저장한다. 따로 빼면 글을 안 쓰고 나간 사진이
 * 스토리지에 고아로 남는다.
 */
@Entity
@Getter
@Table(name = "post_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

	@Id
	@Column(name = "post_image_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "image_url", nullable = false, length = 1000)
	private String imageUrl;

	/** 보낸 배열 순서가 그대로 표시 순서다. */
	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Builder
	private PostImage(Long postId, String imageUrl, Integer sortOrder) {
		this.postId = postId;
		this.imageUrl = imageUrl;
		this.sortOrder = sortOrder;
	}
}
