package mukkeu.mukkeu.analysis.dto;

import java.util.List;

import mukkeu.mukkeu.menu.domain.MenuType;
import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.restaurant.domain.FoodCategory;

/**
 * 영상 분석 응답. 설명용 필드는 담지 않는다. 화면에 그릴 값만 보낸다.
 *
 * exactMatches 는 브랜드 수만큼 나온다. 엽떡 + 교촌 영상이면 둘 다 담긴다.
 *
 * dishResults 는 요리 하나당 하나다. 영상의 각 요리를 근처 어디서 시킬 수 있는지
 * 후보 가게를 나열한다. 한 가게에서 모든 요리를 파는 경우가 드물기 때문에
 * "가게 하나에 조합 전체" 로 묶지 않는다. 결제도 어차피 가게별로 나뉘고
 * checkoutId 로 묶이므로 이쪽이 제품 구조와 맞는다.
 */
public record AnalysisResponse(
	List<ExactMatch> exactMatches,
	List<DishResult> dishResults
) {

	public record ExactMatch(
		String brandName,
		StoreResponse restaurant,
		List<ItemResponse> items,
		int totalPrice
	) {
	}

	/** 요리 하나에 대한 후보 가게 목록. candidates 는 score 내림차순이다. */
	public record DishResult(
		String dishName,
		List<Candidate> candidates
	) {
	}

	/** 가게 하나 + 그 가게에서 이 요리에 가장 가까운 메뉴 하나. */
	public record Candidate(
		StoreResponse restaurant,
		ItemResponse item,
		double score
	) {
	}

	public record StoreResponse(
		Long restaurantId,
		String name,
		FoodCategory foodCategory,
		String area,
		Double rating,
		Integer etaMin,          // 카카오 이동시간 + prep_min. 실패하면 delivery_min
		Integer deliveryFee,
		Integer minOrderPrice,
		Double distanceKm,
		String imageUrl
	) {
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
		List<OptionResponse> options,  // 안 고른 것까지 전부. 화면이 목록을 그려야 한다
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
