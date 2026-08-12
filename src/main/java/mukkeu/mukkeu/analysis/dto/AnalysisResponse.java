package mukkeu.mukkeu.analysis.dto;

import java.util.List;

import mukkeu.mukkeu.analysis.domain.EmptyReason;
import mukkeu.mukkeu.analysis.domain.MatchReasonTag;
import mukkeu.mukkeu.menu.domain.MenuType;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.restaurant.dto.RestaurantSummary;

/**
 * 영상 분석 응답.
 *
 * exactMatches 는 브랜드 수만큼 나온다. 엽떡 + 교촌 영상이면 둘 다 담긴다.
 * 메뉴 단위가 아니라 가게 단위인 이유는 결제가 가게 단위로 쪼개지기 때문이다
 * (orders 행 1개 = 가게 1곳). 카드를 그대로 장바구니에 담는 흐름이 성립하려면
 * 카드도 가게 단위여야 한다.
 *
 * dishResults 는 요리 하나당 하나다. 한 가게에서 모든 요리를 파는 경우가 드물어
 * "가게 하나에 조합 전체" 로 묶지 않는다.
 *
 * 가게 정보(restaurant)는 RestaurantSummary 한 종류로 통일했다. 메뉴판 조회
 * (GET /v1/restaurants/{id}/menus) 도 같은 모양을 쓰므로, 프론트는 가게 카드
 * 컴포넌트를 한 번만 만들면 된다.
 *
 * ── 설명 필드 두 층 ────────────────────────────────────────
 * summary          화면 맨 위 한 줄. LLM 이 쓴다. 실패하면 null 이다.
 * tags / reason    카드마다 왜 이걸 골랐는지. 서버 템플릿이 만든다(MatchReasonTagger).
 *
 * 두 층을 다르게 처리하는 이유는 성격이 다르기 때문이다. 카드 문구는 태그 5개 이하의
 * 유한한 조합이라 템플릿이면 충분하고 늘 일관되지만, 요약은 "요리 3개 중 하나는 브랜드로
 * 잡히고 하나는 후보 5곳, 하나는 배달시간 때문에 0곳" 같은 조합 공간이 커서 if 문으로
 * 덮으면 분기가 지저분해지고 문장이 기계적으로 읽힌다.
 *
 * 그래서 LLM 이 죽어도 카드 문구는 멀쩡히 살아 있다. summary 만 null 이 된다.
 */
public record AnalysisResponse(

	/** 전체 요약 한 줄. LLM 호출 실패·타임아웃이면 null. 빈 결과일 때도 null. */
	String summary,

	/** 결과가 완전히 비었을 때만 채운다. 평소에는 null. 프론트가 값마다 고정 문구를 매핑한다. */
	EmptyReason emptyReason,

	List<ExactMatch> exactMatches,
	List<DishResult> dishResults
) {

	/** 태그·요약을 붙이기 전 단계에서 쓰는 생성자. 조립이 끝난 뒤 채워 넣는다. */
	public AnalysisResponse(List<ExactMatch> exactMatches, List<DishResult> dishResults) {
		this(null, null, exactMatches, dishResults);
	}

	public record ExactMatch(
		String brandName,
		RestaurantSummary restaurant,
		List<ItemResponse> items,
		int totalPrice,

		/** 우선순위 순으로 최대 2개. 프론트가 몇 개까지 배지로 그릴지 정한다. */
		List<MatchReasonTag> tags,

		/** tags 조합을 한 줄로 합친 문구. 태그가 없으면 null. */
		String reason
	) {

		/** 태그 부여 전 생성자. MatchReasonTagger 가 나중에 채운다. */
		public ExactMatch(String brandName, RestaurantSummary restaurant,
			List<ItemResponse> items, int totalPrice) {
			this(brandName, restaurant, items, totalPrice, List.of(), null);
		}
	}

	/** 요리 하나에 대한 후보 가게 목록. candidates 는 score 내림차순이다. */
	public record DishResult(
		String dishName,
		List<Candidate> candidates
	) {
	}

	/** 가게 하나 + 그 가게에서 이 요리에 가장 가까운 메뉴 하나. */
	public record Candidate(
		RestaurantSummary restaurant,
		ItemResponse item,
		double score,

		/** 우선순위 순으로 최대 2개. 내세울 것이 없으면 빈 배열이다. */
		List<MatchReasonTag> tags,

		/** tags 조합을 한 줄로 합친 문구. 태그가 없으면 null. */
		String reason
	) {

		/** 태그 부여 전 생성자. MatchReasonTagger 가 나중에 채운다. */
		public Candidate(RestaurantSummary restaurant, ItemResponse item, double score) {
			this(restaurant, item, score, List.of(), null);
		}
	}

	public record ItemResponse(
		Long menuId,
		String name,
		MenuType menuType,
		int price,
		String imageUrl,
		int quantity,
		SpiceLevel spiceLevel,
		boolean spiceAdjustable,
		SpiceLevel selectedSpice,      // 조절 불가 메뉴는 null
		List<OptionResponse> options,  // 영상에서 언급돼 켜진 것만. 나머지는 메뉴판 API 에서
		int optionsPrice,
		int lineTotal
	) {
	}

	public record OptionResponse(
		String group,
		String name,
		int price,
		boolean selected
	) {
	}
}
