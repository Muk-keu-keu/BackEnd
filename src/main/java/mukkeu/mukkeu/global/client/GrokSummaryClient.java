package mukkeu.mukkeu.global.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * OCI Generative AI(xAI Grok)로 분석 결과의 문장을 만든다.
 *
 * 만드는 것이 둘이다.
 *   summary   화면 맨 위 한 줄 요약
 *   reasons   카드마다 "왜 이걸 골랐나" 한 줄
 * 둘을 한 호출로 받는다. 재료가 거의 같아 두 번 부르면 같은 것을 두 번 보내는 셈이고,
 * 한 번에 받으면 모델이 전체 그림을 보고 써서 요약과 카드 문구가 서로 어긋나지 않는다.
 *
 * SDK 를 쓰지 않는다. OCI Generative AI 는 OpenAI 호환 엔드포인트와 API 키(Bearer)를
 * 지원하므로 URL 하나에 POST 한 번이면 끝난다. Object Storage 를 PAR 로 붙인 것과 같은
 * 판단이다 — SDK 를 붙이면 의존성이 수십 MB 늘고 tenancy·user·fingerprint·private key 를
 * 서버 환경변수로 또 넣어야 한다.
 *
 * ── 태그는 만들지 않는다 ──
 * 카드의 tags 는 MatchReasonTagger 가 결정론으로 정한다. 배지는 사용자가 카드를 비교하는
 * 기준이라 같은 입력에 같은 값이 나와야 한다. 여기서 만드는 것은 문장뿐이다.
 *
 * ── 실패하면 null 이다. 예외를 던지지 않는다 ──
 * KakaoEtaClient 와 같은 태도다. 부가 정보 하나 때문에 검색 결과 전체를 버릴 이유가 없다.
 * 호출자는 문장을 못 받은 카드에 템플릿 문구를 그대로 둔다.
 *
 * ── 모델 선택 ──
 * 기본값은 grok-4.20 non-reasoning 이다. 추론 능력이 아니라 타임아웃 안에 들어오는 속도가
 * 품질을 결정한다. 타임아웃에 걸려 문장이 전부 null 이 되면 좋은 모델을 쓴 의미가 사라진다.
 * 비용이 제약이 아니어도 지연시간은 여전히 제약이다.
 */
@Slf4j
@Component
public class GrokSummaryClient {

	private static final String SYSTEM_PROMPT = """
		너는 영상 속 음식을 근처 배달 가게와 연결해주는 서비스의 문구를 쓴다.
		입력 JSON 은 분석 결과다. 화면 맨 위 요약 하나와, 가게 카드마다 추천 이유 한 줄을 만든다.

		[요약 규칙]
		1. 2문장 이내, 전체 90자 이내.
		2. exact 가 있는 요리는 그 가게에서 그대로 시킬 수 있다는 점을 먼저 말한다.
		   이때 요리는 brandName 으로 부르고, 가게 이름은 store 를 통째로 쓴다. 줄이지 않는다.
		3. 후보가 0개인 요리는 반드시 언급하고, emptyReason 에 맞는 이유를 덧붙인다.
		4. 개수만 세어 붙이지 않는다. "4곳 찾았어요" 로 끝내지 말고, 후보들이 영상과 어떻게
		   다른지를 한 마디로 요약한다. 예를 들어 값이 더 싼 쪽인지, 재료가 다른 쪽인지.
		5. 요약에 후보 가게 이름을 한 곳까지는 써도 된다. 나열하지는 않는다.

		[카드 이유 규칙]
		6. exact 와 candidates 의 모든 항목에 하나씩 쓴다. id 를 그대로 돌려준다.
		7. 한 문장, 40자 이내.
		8. rank 가 1 인 카드는 왜 이것이 제일 나은지 쓴다.
		   나머지 카드에는 "그래도 이걸 골라도 되는 이유" 를 반드시 하나 넣는다.
		   ★ 영상과 다르다는 말만 하고 끝내지 마라. 그러면 그 카드가 목록에 있는 이유가 사라진다.
		     다른 점을 말했으면 "그래도 ~", "대신 ~" 을 붙여 고를 이유를 준다.
		9. 근거는 menu(메뉴 이름)·aliases(별칭)·price(가격)·menuOptions(고를 수 있는 옵션)
		   ·matchedOptions(영상에서 말한 옵션 중 맞은 것)에서 찾는다.
		10. description 이나 tasteTags 가 후보들끼리 비슷하면 그것을 근거로 쓰지 않는다.
		    그 경우 이름·별칭·옵션·가격의 차이를 짚는다.
		11. 카드마다 서로 다른 점을 짚되, 차이가 실제로 없으면 없다고 말한다.
		    ★ 다른 점을 만들어 내지 마라. 값도 옵션도 같으면 "앞 가게와 조건이 거의 같아요"
		      처럼 쓰는 것이 옳다. 억지로 다르게 쓰려고 없는 맛·식감·인기를 지어내면 안 된다.
		12. 문장의 시작과 맺음을 카드마다 바꾼다. 한 응답 안에서
		    ★ "영상과 달라요" 로 끝나는 문장은 한 개까지만 쓴다.
		    ★ "가격은 비슷하지만" 처럼 같은 말로 시작하는 문장을 두 번 쓰지 않는다.
		    카드가 말할 수 있는 것은 여러 가지다. 카드마다 아래에서 다른 것을 고른다.
		      이 메뉴만의 재료·형태   "통오징어가 들어가서 양이 푸짐해요"
		      값 비교               "5천원이라 부담이 제일 적어요"
		      고를 수 있는 옵션       "순살로 바꿀 수도 있어요"
		      앞 카드와의 관계        "앞 가게랑 조건이 거의 같아요"
		      영상과의 거리          "결은 좀 다른데 ~"   ← 이것만 반복하지 마라

		[공통 금지]
		13. 거리·시간·평점·리뷰 수는 언급하지 않는다. 화면에 숫자로 이미 나가 있다.
		14. 입력 JSON 에 글자로 적혀 있지 않은 것은 쓰지 않는다. 특히 아래는 절대 금지다.
		    - 맛·식감 묘사   "달달해요" "국물이 없어요" "촉촉해요" (tasteTags 에 있는 말만 허용)
		    - 인기·평판     "인기가 많아요" "덜 인기예요" "유명해요" "많이들 시켜요"
		    - 조리법·원산지  "직화로 구워요" "국내산이에요"
		    메뉴 이름에 든 말은 써도 된다. 예를 들어 이름이 '허니콤보' 면 달콤하다고 말해도 된다.
		15. 예시(few-shot)에 나온 가게·메뉴 이름은 예시일 뿐이다. 입력 JSON 에 없으면
		    문장에 절대 등장시키지 마라. 다른 카드와 비교할 때도 입력에 있는 가게만 언급한다.
		16. 숫자는 입력에 있는 값만 쓴다.
		17. 모든 문장을 "~해요" 로 끝낸다. "~습니다", "~다" 로 끝내지 않는다.
		18. 친구에게 하나 골라 주듯 말한다. 사전 설명이나 스펙 비교표처럼 쓰지 않는다.
		    딱딱함  "가격은 비슷하지만 오징어가 들어가 영상 떡볶이와 다릅니다"
		    자연스러움  "여긴 오징어가 통째로 들어가서 영상이랑은 좀 다른데 양은 훨씬 많아요"
		    "여긴", "이건", "대신", "그래도" 같은 말을 자연스럽게 섞고, 조사를 생략하지 않는다.
		    사과하거나 변명하지 않는다.

		[emptyReason]
		NO_NEARBY               반경 안에 가게가 없음
		DELIVERY_TIME_FILTERED  배달시간 조건에 다 걸림
		NO_SIMILAR_MENU         조건 안에서 비슷한 메뉴를 파는 가게를 못 찾음

		[출력]
		아래 형태의 JSON 만 출력한다. 다른 텍스트를 붙이지 않는다.
		{"summary":"...","reasons":[{"id":"...","reason":"..."}]}
		""";

	/**
	 * 규칙만으로는 톤이 매번 달라진다. 예시가 규칙보다 세게 작동하므로 하나를 붙인다.
	 *
	 * ★ 가게·메뉴 이름을 전부 가상으로 쓴다. 실명(BHC, 네네치킨)을 썼더니 모델이 예시를
	 *   입력으로 착각해, 후보에 있지도 않은 "BHC보다 순살 변경이 덜 인기예요" 를 만들어 냈다.
	 *   시드에 없는 이름이라 이제 그런 문장이 나오면 오염이라는 것이 바로 드러난다.
	 *
	 * 예시 하나에 세 가지를 담았다.
	 *   영상과 다른 점을 밝히는 문장(규칙 8) — 글로만 적으면 모델이 전부 칭찬으로 채운다
	 *   재료가 달라 결이 다르다고 말하는 문장(규칙 9)
	 *   차이가 없을 때 없다고 말하는 문장(규칙 11) — 이게 없으면 없는 차이를 지어낸다
	 */
	private static final List<Message> FEW_SHOT = List.of(
		new Message("user", """
			{"videoText":"가나피자에서 갈릭 추가해서 먹었어요","dishes":[{"name":"피자","brandName":"가나피자",\
			"videoDescription":"치즈가 늘어지는 피자","videoOptions":["갈릭 소스"],\
			"exact":{"id":"e1","store":"가나피자 1호점","menu":"치즈피자","price":19000,"matchedOptions":["갈릭 소스"]},\
			"candidates":[{"id":"d0r11","rank":1,"store":"한입파스타 2호점","menu":"고구마피자",\
			"aliases":"고구마무스피자, 스위트피자","price":19000,"menuOptions":["갈릭 소스","치즈 추가"],"matchedOptions":["갈릭 소스"]},\
			{"id":"d0r12","rank":2,"store":"골목국수 3호점","menu":"고구마피자",\
			"aliases":"고구마무스피자","price":19000,"menuOptions":["갈릭 소스","치즈 추가"],"matchedOptions":["갈릭 소스"]}]}]}"""),
		new Message("assistant", """
			{"summary":"영상 속 가나피자는 1호점에서 그대로 시킬 수 있어요. 비슷한 곳도 있는데 고구마 쪽이라 결이 달라요",\
			"reasons":[{"id":"e1","reason":"영상에 나온 그 피자고 갈릭 소스까지 그대로 담을 수 있어요"},\
			{"id":"d0r11","reason":"고구마가 올라가서 영상이랑 좀 다른데 갈릭 소스는 그대로 들어가요"},\
			{"id":"d0r12","reason":"앞 가게랑 값도 옵션도 같아서 둘 중 아무거나 골라도 돼요"}]}"""),
		new Message("user", """
			{"videoText":"매콤한 비빔국수 곱빼기로 먹었어요","dishes":[{"name":"국수",\
			"videoDescription":"매콤한 비빔국수","videoOptions":["곱빼기"],\
			"candidates":[{"id":"d0r21","rank":1,"store":"한입국수 1호점","menu":"매운비빔국수",\
			"aliases":"매비국","price":8000,"menuOptions":["곱빼기","계란 추가"],"matchedOptions":["곱빼기"]},\
			{"id":"d0r22","rank":2,"store":"가나국수 2호점","menu":"열무비빔국수",\
			"aliases":"열무국수","price":7000,"menuOptions":["곱빼기"],"matchedOptions":["곱빼기"]},\
			{"id":"d0r23","rank":3,"store":"골목분식 3호점","menu":"잔치국수",\
			"price":6000,"menuOptions":[],"matchedOptions":[]},\
			{"id":"d0r24","rank":4,"store":"한입국수 4호점","menu":"매운비빔국수",\
			"aliases":"매비국","price":8000,"menuOptions":["곱빼기","계란 추가"],"matchedOptions":["곱빼기"]}]}]}"""),
		new Message("assistant", """
			{"summary":"영상에 나온 가게는 근처에 없지만, 같은 매운 비빔국수부터 더 싼 국수집까지 있어요",\
			"reasons":[{"id":"d0r21","reason":"영상에서 본 그 매운 비빔국수라 곱빼기까지 그대로예요"},\
			{"id":"d0r22","reason":"열무가 들어가서 맛 결은 좀 다른데 천원 더 싸요"},\
			{"id":"d0r23","reason":"여긴 국물국수라 영상이랑 꽤 다른데 6천원으로 제일 저렴해요"},\
			{"id":"d0r24","reason":"1번이랑 메뉴도 값도 옵션도 똑같아요"}]}"""));

	/** 요약 90자 + 카드 문구 40자 × 최대 15장. 여유를 두고 잡는다. */
	private static final int MAX_TOKENS = 1200;

	/** 같은 입력에 문장이 크게 흔들리지 않을 정도로만 낮춘다. */
	private static final double TEMPERATURE = 0.3;

	/** 프롬프트를 고쳐야 한다는 신호. 넘겼다고 서버가 자르지는 않는다. */
	private static final int SUMMARY_MAX_CHARS = 90;
	private static final int REASON_MAX_CHARS = 40;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String baseUrl;
	private final String model;
	private final boolean enabled;

	public GrokSummaryClient(
		@Value("${oci.genai.api-key:}") String apiKey,
		@Value("${oci.genai.base-url:}") String baseUrl,
		@Value("${oci.genai.model:xai.grok-4.20-0309-non-reasoning}") String model,
		@Value("${oci.genai.timeout-seconds:3}") int timeoutSeconds,
		ObjectMapper objectMapper) {

		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.model = model;
		this.objectMapper = objectMapper;
		this.enabled = !apiKey.isBlank() && !baseUrl.isBlank();

		// 이 호출은 분석 응답을 막고 있다. 넉넉히 잡으면 사용자가 그만큼 더 기다린다.
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(2));
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

		this.restClient = RestClient.builder()
			.baseUrl(baseUrl.isBlank() ? "http://localhost" : baseUrl)
			.requestFactory(factory)
			.build();

		// 켜졌든 꺼졌든 항상 남긴다. "조용히 아무 일도 안 함" 은 진단할 수가 없다.
		if (enabled) {
			log.info("분석 문장 생성 ON  model={} baseUrl={} key={}", model, baseUrl, mask(apiKey));
		} else {
			log.warn("분석 문장 생성 OFF (요약은 null, 카드 문구는 템플릿). apiKey={} baseUrl={}",
				apiKey.isBlank() ? "없음" : "있음", baseUrl.isBlank() ? "없음" : baseUrl);
		}
	}

	/**
	 * @return 요약과 카드별 문구. 설정이 없거나 호출·파싱이 실패하면 {@link Narration#empty()}.
	 *         빈 결과는 정상 흐름이다. 호출자는 못 받은 카드에 템플릿 문구를 그대로 둔다.
	 */
	public Narration narrate(SummaryRequest request) {
		if (!enabled) {
			log.debug("분석 문장 생성이 꺼져 있어 건너뛴다. 기동 로그의 'OFF' 를 확인하라.");
			return Narration.empty();
		}
		if (request == null || request.dishes() == null || request.dishes().isEmpty()) {
			log.debug("문장을 만들 요리가 없다.");
			return Narration.empty();
		}

		try {
			String payload = objectMapper.writeValueAsString(request);

			List<Message> messages = new ArrayList<>();
			messages.add(new Message("system", SYSTEM_PROMPT));
			messages.addAll(FEW_SHOT);
			messages.add(new Message("user", payload));

			ChatResponse response = restClient.post()
				.uri("/chat/completions")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ChatRequest(model, messages, TEMPERATURE, MAX_TOKENS))
				.retrieve()
				.body(ChatResponse.class);

			String content = extractContent(response);
			if (content == null) {
				log.warn("문장 응답이 비어 있다. 템플릿 문구로 진행한다.");
				return Narration.empty();
			}
			return readNarration(content);

		} catch (org.springframework.web.client.RestClientResponseException e) {
			// 403 이면 IAM 정책, 404 면 리전·모델명, 401 이면 키 자체를 의심한다.
			log.warn("분석 문장 호출 실패 status={} body={}",
				e.getStatusCode(), e.getResponseBodyAsString());
			return Narration.empty();
		} catch (Exception e) {
			log.warn("분석 문장 생성 실패({}). 템플릿 문구로 진행한다: {}",
				e.getClass().getSimpleName(), e.getMessage());
			return Narration.empty();
		}
	}

	/** 로그에 키를 통째로 남기지 않는다. 앞뒤만 보여도 어느 키인지는 알아볼 수 있다. */
	private static String mask(String key) {
		if (key == null || key.length() < 10) {
			return "***";
		}
		return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
	}

	private String extractContent(ChatResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			return null;
		}
		Choice choice = response.choices().get(0);
		return (choice == null || choice.message() == null) ? null : choice.message().content();
	}

	/**
	 * 모델이 JSON 만 뱉으라는 지시를 어기고 ```json 으로 감싸는 경우가 있다.
	 * 중괄호 구간만 잘라내 파싱한다. 그래도 실패하면 빈 결과다.
	 */
	private Narration readNarration(String content) {
		int from = content.indexOf('{');
		int to = content.lastIndexOf('}');
		if (from < 0 || to <= from) {
			log.warn("문장 응답에서 JSON 을 찾지 못했다: {}", content);
			return Narration.empty();
		}

		NarrationBody body;
		try {
			body = objectMapper.readValue(content.substring(from, to + 1), NarrationBody.class);
		} catch (Exception e) {
			log.warn("문장 JSON 파싱 실패: {}", content);
			return Narration.empty();
		}
		if (body == null) {
			return Narration.empty();
		}

		String summary = blankToNull(body.summary());
		if (summary != null && summary.length() > SUMMARY_MAX_CHARS) {
			// 자르지 않는다. 문장 중간에서 끊기면 안 쓰느니만 못하다. 튜닝 신호로만 남긴다.
			log.warn("요약이 {}자로 제한({}자)을 넘었다: {}", summary.length(), SUMMARY_MAX_CHARS, summary);
		}

		Map<String, String> reasons = new LinkedHashMap<>();
		int tooLong = 0;
		if (body.reasons() != null) {
			for (ReasonBody item : body.reasons()) {
				if (item == null || item.id() == null || item.id().isBlank()) {
					continue;
				}
				String reason = blankToNull(item.reason());
				if (reason == null) {
					continue;
				}
				if (reason.length() > REASON_MAX_CHARS) {
					tooLong++;
				}
				reasons.put(item.id().trim(), reason);
			}
		}
		if (tooLong > 0) {
			log.warn("카드 문구 {}개가 제한({}자)을 넘었다", tooLong, REASON_MAX_CHARS);
		}

		// 한 응답 안에서 같은 문장이 반복되면 "템플릿 같다" 는 신호다. 프롬프트를 조일 근거로 남긴다.
		long distinct = reasons.values().stream().distinct().count();
		if (reasons.size() >= 2 && distinct < reasons.size()) {
			log.warn("카드 문구 {}개 중 서로 다른 것이 {}개뿐이다", reasons.size(), distinct);
		}

		log.debug("문장 생성 완료. summary={} reasons={}", summary != null, reasons.size());
		return new Narration(summary, reasons);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

	// ── 우리가 만들어 보내는 입력 ─────────────────────────────
	//    카드가 화면에 이미 보여주는 값(거리·ETA·평점·score)은 넣지 않는다. 넣으면 모델이
	//    그것을 문장에 되읽어서, 카드 옆 숫자를 한 번 더 말할 뿐인 문구가 나온다.
	//    대신 카드가 보여주지 못하는 값(별칭·전체 옵션·영상 원문)을 넣는다.

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SummaryRequest(
		String videoText,                 // 캡션 원문. 영상의 말투가 문장에 묻어나게 한다
		List<DishSummary> dishes,
		Preferences preferences
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DishSummary(
		String name,
		String brandName,                 // 영상에서 부른 브랜드. 없으면 null
		String videoDescription,          // 영상에서 이 요리를 묘사한 말
		List<String> videoOptions,        // 영상에서 언급된 옵션
		ExactSummary exact,               // 브랜드 매칭 성공 시. 실패면 null
		List<CandidateSummary> candidates,
		int candidateCount,
		String emptyReason                // candidateCount == 0 일 때만
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ExactSummary(
		String id,
		String store,
		List<String> menus,
		int price,
		List<String> matchedOptions
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CandidateSummary(
		String id,
		int rank,                         // 응답에 실린 순서. 1 이 서버가 꼽은 최선이다
		String store,
		String menu,
		String aliases,                   // 이 메뉴를 달리 부르는 말. 차이를 짚는 핵심 재료
		String description,
		String tasteTags,
		int price,
		List<String> menuOptions,         // 이 메뉴에서 고를 수 있는 옵션 전체
		List<String> matchedOptions       // 그중 영상에서 언급돼 켜진 것
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Preferences(String maxSpiceLevel, Integer maxDeliveryMin, Boolean excludeMeat) {
	}

	// ── 우리가 돌려받는 결과 ──────────────────────────────────

	/**
	 * @param summary 화면 맨 위 한 줄. 못 받았으면 null
	 * @param reasons 카드 id → 문구. 못 받은 카드는 아예 키가 없다(호출자가 템플릿을 유지한다)
	 */
	public record Narration(String summary, Map<String, String> reasons) {

		private static final Narration EMPTY = new Narration(null, Map.of());

		public static Narration empty() {
			return EMPTY;
		}

		/** 못 받았으면 null 을 준다. 호출자가 기존 문구를 그대로 두면 된다. */
		public String reasonOf(String id) {
			return reasons.get(id);
		}
	}

	// ── OpenAI 호환 요청/응답 ────────────────────────────────
	//    필드명이 snake_case 라 @JsonProperty 로 맞춘다.
	//    (Jackson 3 에서도 애노테이션 패키지는 com.fasterxml 그대로다)

	private record Message(String role, String content) {
	}

	private record ChatRequest(
		String model,
		List<Message> messages,
		double temperature,
		@JsonProperty("max_tokens") int maxTokens
	) {
	}

	private record ChatResponse(@JsonProperty("choices") List<Choice> choices) {
	}

	private record Choice(@JsonProperty("message") ResponseMessage message) {
	}

	private record ResponseMessage(@JsonProperty("content") String content) {
	}

	private record NarrationBody(String summary, List<ReasonBody> reasons) {
	}

	private record ReasonBody(String id, String reason) {
	}
}
