package mukkeu.mukkeu.restaurant.domain;

import java.util.Locale;
import java.util.Map;

/**
 * restaurant.food_category 의 CHECK 제약과 값이 1:1로 같아야 한다.
 *
 * 바깥에서 들어온 문자열을 이 enum 으로 해석하는 규칙도 여기 둔다. 프론트가 보내는 표기와
 * 우리 값 사이의 대응은 이 타입의 사정이지, 그 값을 쓰는 쪽(분석·검색)이 알아야 할 일이 아니다.
 * 서비스마다 자기 표를 들고 있으면 하나가 어긋나는 순간 화면마다 다른 결과가 나온다.
 */
public enum FoodCategory {

	KOREAN, CHINESE, JAPANESE, WESTERN, SNACK,
	CHICKEN, PIZZA, ASIAN, CAFE_DESSERT;

	/**
	 * 프론트가 보내는 표기 → 우리 값.
	 *
	 * 한국어 영상에서 뽑는 값이라 "분식", "치킨" 이 오는 것이 자연스럽다. 이걸 프론트더러
	 * enum 이름으로 바꿔 보내라고 하면 클라이언트마다 같은 표를 들고 있어야 하고,
	 * 하나가 어긋나는 순간 조건이 조용히 사라진다. 그래서 서버가 맞춘다.
	 *
	 * 표는 일부러 좁다. 카테고리가 틀리면 그 카테고리 가게만 검색 대상이 되어 정답 가게가
	 * 통째로 빠지므로, 애매한 말을 억지로 끼워 맞추기보다 못 알아듣는 편이 낫다.
	 * "떡볶이" 같은 메뉴 이름을 넣지 않은 이유다 — 분식집에도 있고 한식집에도 있다.
	 */
	private static final Map<String, FoodCategory> ALIASES = Map.ofEntries(
		Map.entry("한식", KOREAN),
		Map.entry("백반", KOREAN),
		Map.entry("중식", CHINESE),
		Map.entry("중국집", CHINESE),
		Map.entry("일식", JAPANESE),
		Map.entry("초밥", JAPANESE),
		Map.entry("양식", WESTERN),
		Map.entry("햄버거", WESTERN),
		Map.entry("버거", WESTERN),
		Map.entry("분식", SNACK),
		Map.entry("치킨", CHICKEN),
		Map.entry("피자", PIZZA),
		Map.entry("아시안", ASIAN),
		Map.entry("아시아", ASIAN),
		Map.entry("카페", CAFE_DESSERT),
		Map.entry("디저트", CAFE_DESSERT),
		Map.entry("카페디저트", CAFE_DESSERT),
		Map.entry("CAFEDESSERT", CAFE_DESSERT));

	/**
	 * enum 이름 → 한국어 별칭 순으로 본다.
	 *
	 * @return 못 알아들으면 null. 예외를 던지지 않는 이유는 실패 방향 때문이다.
	 *         모르는 값에 조건을 걸면 결과가 0 개가 되어 사용자가 이유를 알 수 없지만,
	 *         조건을 걸지 않으면 후보가 조금 넓어질 뿐이다. 호출자가 그렇게 처리하도록 둔다.
	 */
	public static FoodCategory from(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String raw = value.trim();
		try {
			return valueOf(raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			// enum 이름이 아니다. 별칭표를 본다.
		}

		// "카페 디저트", "cafe-dessert" 처럼 띄어쓰기·구분자만 다른 표기를 한 모양으로 모은다.
		return ALIASES.get(raw.replaceAll("[\\s_-]", "").toUpperCase(Locale.ROOT));
	}
}
