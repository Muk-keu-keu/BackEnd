package mukkeu.mukkeu.menu.app;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import mukkeu.mukkeu.menu.domain.MenuOption;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 영상에서 들린 표현으로 메뉴 옵션을 자동 선택한다.
 *
 * 긴 이름부터 검사하고 매칭된 자리를 지운다.
 *   그냥 contains 로 각각 검사하면 "치즈볼 추가" 에서 "치즈" 까지 켜진다.
 *   긴 것에 먼저 기회를 주고 그 자리를 소비하면 이 오탐이 사라지고,
 *   "치즈볼이랑 치즈도" 처럼 둘 다 원한 경우는 그대로 둘 다 켜진다.
 *
 * 애매하면 켜지 않는다.
 *   잘못 켜진 옵션은 사용자가 못 보고 넘어가면 결제액이 올라가지만,
 *   안 켜진 것은 화면에서 직접 켜면 그만
 *
 * 두 방향을 본다.
 *   1단계  문구가 옵션 이름을 품는가   "분모자 사리 넣어서" ⊃ "분모자 사리"
 *   2단계  옵션 이름이 문구를 품는가   "분모자 사리" ⊃ "분모자"
 *   프론트는 옵션을 명사구로 정리해 보낸다. 그러면 문구가 옵션 이름보다 짧아져
 *   1단계로는 절대 못 잡는다. 긴 문자열이 짧은 문자열 안에 들어갈 수 없기 때문이다.
 *   옵션 이름을 자르거나 접미사 사전을 두지 않고 방향만 하나 더 본다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionMatcher {

	private static final TypeReference<List<MenuOption>> OPTION_LIST = new TypeReference<>() {};

	/** 2단계에서 쓸 문구의 최소 길이. 한 글자는 어느 옵션 이름에나 걸린다. */
	private static final int MIN_PHRASE_LENGTH = 2;

	private final ObjectMapper objectMapper;

	/** menu.options JSON 을 파싱한다. 비어 있으면 빈 목록. */
	public List<MenuOption> parse(String optionsJson) {
		if (optionsJson == null || optionsJson.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(optionsJson, OPTION_LIST);
		} catch (Exception e) {
			// DB 제약이 막아 주지만, 뚫렸더라도 검색 전체가 죽으면 안 된다.
			log.warn("menu.options 파싱 실패. 옵션 없이 진행한다: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * 영상 문구에 언급된 옵션 이름들을 골라낸다.
	 *
	 * @param options      그 메뉴가 가진 옵션 전부
	 * @param videoPhrases 영상에서 뽑힌 표현. "분모자 넣어서" 처럼 정제되지 않은 문장
	 *
	 *   - ["분모자", "치즈맛 소스"] → 분모자치즈맛소스 → 옵션 "분모자 치즈맛" 오탐 켜짐 (추가한 내용임 나중에 블로그 작성)
	 *   - → 분모자\n치즈맛소스 → \n 이 벽이 돼서 매칭 실패, 안 켜짐
	 */
	public Set<String> pickMentioned(List<MenuOption> options, List<String> videoPhrases) {
		if (options.isEmpty() || videoPhrases == null || videoPhrases.isEmpty()) {
			return Set.of();
		}

		// 문구별로 먼저 정규화하고 줄바꿈으로 잇는다. 합친 뒤에 정규화하면 문구 사이
		// 공백까지 지워져 경계가 사라진다. ["분모자", "치즈맛 소스"] 가 "분모자치즈맛소스"
		// 가 되면서 언급된 적 없는 "분모자 치즈맛" 옵션이 켜진다.
		String remaining = videoPhrases.stream()
			.map(OptionMatcher::normalize)
			.collect(Collectors.joining("\n"));
		Set<String> picked = new HashSet<>();

		List<MenuOption> usable = options.stream()
			.filter(o -> o.name() != null && !o.name().isBlank())
			.toList();
		if (usable.isEmpty()) {
			return picked;
		}

		// ── 1단계 : 문구가 옵션 이름을 통째로 품는가 ────────────────
		List<MenuOption> byLengthDesc = usable.stream()
			.sorted(Comparator.comparingInt((MenuOption o) -> o.name().length()).reversed())
			.toList();

		for (MenuOption option : byLengthDesc) {
			String name = normalize(option.name());
			if (!name.isEmpty() && remaining.contains(name)) {
				picked.add(option.name());
				remaining = remaining.replace(name, " ");   // 자리를 소비한다
			}
		}

		// ── 2단계 : 옵션 이름이 문구를 품는가 ──────────────────────
		// 프론트가 명사구로 보내면 문구가 더 짧아 1단계로는 못 잡는다.
		//   "분모자".contains("분모자사리")  → false
		//   "분모자사리".contains("분모자")  → true
		for (String phrase : videoPhrases) {
			String key = normalize(phrase);
			if (key.length() < MIN_PHRASE_LENGTH) {
				continue;
			}

			List<MenuOption> hits = usable.stream()
				.filter(o -> !picked.contains(o.name()))
				.filter(o -> normalize(o.name()).contains(key))
				.toList();

			// 후보가 둘 이상이면 켜지 않는다. "치즈" 는 "치즈 사리" 와 "치즈볼 4개" 에
			// 둘 다 걸리는데 어느 쪽인지 알 방법이 없다. 여기서 찍으면 사용자가 시키지도
			// 않은 옵션의 값이 결제액에 붙는다.
			if (hits.size() == 1) {
				picked.add(hits.get(0).name());
			}
		}

		return picked;
	}

	/** 공백을 지우고 소문자로. "당면 사리" 와 "당면사리" 를 같게 본다. */
	private static String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
	}
}
