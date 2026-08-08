package mukkeu.mukkeu.order.dto;

import mukkeu.mukkeu.order.domain.SourcePlatform;

/**
 * 영상 출처. 목록 카드와 상세가 같은 모양을 쓴다.
 *
 * 이 값은 결제 단위 정보인데 orders 행마다 복사되어 저장된다. checkout 테이블을
 * 따로 두지 않기로 했기 때문이고, 읽을 때는 묶음의 첫 행 것만 쓴다.
 */
public record SourceResponse(
	SourcePlatform platform,
	String url,
	String thumbnailUrl,
	String title
) {
}
