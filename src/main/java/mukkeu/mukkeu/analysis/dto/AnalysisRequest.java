package mukkeu.mukkeu.analysis.dto;

import java.util.List;

import mukkeu.mukkeu.menu.domain.SpiceLevel;
import mukkeu.mukkeu.order.domain.SourcePlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 영상 분석 요청. 프론트가 영상에서 뽑아낸 것을 그대로 보낸다.
 *
 * dishes[].options 는 정규화하지 않고 들린 대로 보낸다.
 *   "분모자 넣어서" 같은 문장 그대로다. 프론트마다 정규화 기준이 달라지면
 *   서버가 맞출 수 없으므로, 매칭 규칙은 서버 한 곳에만 둔다.
 *
 * brandName 은 메뉴마다 붙는다.
 *   엽떡 떡볶이와 교촌 치킨을 같이 먹는 영상이 흔한데, 최상위에 하나만 두면
 *   둘째 가게를 표현할 방법이 없다.
 */
public record AnalysisRequest(

	@Valid @NotNull Source source,
	@Valid @NotNull Extracted extracted,
	@Valid Preferences preferences
) {

	public record Source(
		@NotNull SourcePlatform platform,
		@NotBlank String url,
		String rawText
	) {
	}

	public record Extracted(
		@Valid @NotEmpty List<Dish> dishes,
		List<String> keywords
	) {
	}

	public record Dish(
		@NotBlank String name,
		String brandName,
		String restaurantName,
		String foodCategory,
		String description,
		List<String> options
	) {
	}

	/** 사용자가 켠 조건. 어느 것도 서버가 임의로 완화하지 않는다. */
	public record Preferences(
		SpiceLevel maxSpiceLevel,
		Integer maxDeliveryMin,
		Boolean excludeMeat
	) {

		public boolean isExcludeMeat() {
			return Boolean.TRUE.equals(excludeMeat);
		}
	}
}
