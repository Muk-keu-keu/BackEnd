package mukkeu.mukkeu.post.dto;

import java.time.OffsetDateTime;
import java.util.List;

import mukkeu.mukkeu.order.dto.OrderDetailResponse;

/**
 * 요기족보 단건.
 *
 * order 에 그 글이 참조하는 결제 내역이 통째로 들어간다. 결제 상세와 같은 모양이라
 * "이 조합 따라 담기" 를 누르면 프론트가 그대로 장바구니에 넣을 수 있다.
 * 따라담기에 별도 API 를 두지 않는 이유가 이것이다 — 장바구니는 프론트 상태이고
 * 결제는 이미 POST v1/orders 가 있다.
 */
public record PostDetailResponse(
	Long postId,
	String title,
	String body,
	List<String> imageUrls,
	String authorNickName,
	Long authorId,
	OffsetDateTime createdAt,
	int likeCount,
	int commentCount,
	boolean liked,
	boolean mine,                        // 수정·삭제 버튼을 그릴지
	OrderDetailResponse order            // 이 글이 자랑하는 그 결제
) {
}
