package mukkeu.mukkeu.global.client;

import java.time.Duration;
import java.util.List;

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
 * OCI Generative AI(xAI Grok)로 분석 결과 요약 한 줄을 만든다.
 *
 * SDK 를 쓰지 않는다. OCI Generative AI 는 OpenAI 호환 엔드포인트와 API 키(Bearer)를
 * 지원하므로 URL 하나에 POST 한 번이면 끝난다. Object Storage 를 PAR 로 붙인 것과 같은
 * 판단이다 — SDK 를 붙이면 의존성이 수십 MB 늘고 tenancy·user·fingerprint·private key 를
 * 서버 환경변수로 또 넣어야 한다.
 *
 * ── 이 호출이 하는 일은 요약 하나뿐이다 ──
 * 카드마다 붙는 태그·문구는 MatchReasonTagger 가 만든다. 태그는 5개 이하의 유한한 조합이라
 * 템플릿이면 충분하고, LLM 에 맡기면 같은 후보에 매번 다른 배지가 붙어 배지 자체를 믿을 수
 * 없게 된다. LLM 은 조합 공간이 큰 요약만 담당한다.
 *
 * ── 실패하면 null 이다. 예외를 던지지 않는다 ──
 * KakaoEtaClient 와 같은 태도다. 부가 정보 하나 때문에 검색 결과 전체를 버릴 이유가 없다.
 * 요약이 없으면 화면 맨 윗줄만 비고 카드는 그대로 나간다.
 *
 * ── 모델 선택 ──
 * 기본값은 grok-4.20 non-reasoning 이다. 플래그십(grok-4.3)이 더 똑똑하지만 이 작업은
 * 작은 JSON 을 받아 90자 문장 하나를 쓰는 일이라 추론 능력이 아니라 타임아웃 안에 들어오는
 * 속도가 품질을 결정한다. 추론 모델은 답하기 전 사고 과정을 거쳐 지연이 늘고, 타임아웃에
 * 걸려 요약이 null 이 되면 좋은 모델을 쓴 의미가 사라진다.
 * 비용이 제약이 아니어도 지연시간은 여전히 제약이다.
 */
@Slf4j
@Component
public class GrokSummaryClient {

	private static final String SYSTEM_PROMPT = """
		너는 영상 속 음식을 근처 배달 가게와 연결해주는 서비스의 요약 문구를 쓴다.
		입력 JSON 은 분석 결과다. 사용자가 화면 맨 위에서 한눈에 읽을 요약으로 바꿔라.

		[규칙]
		1. 2문장 이내, 전체 90자 이내.
		2. "~해요" 톤. 사과하거나 변명하지 않는다.
		3. 입력 JSON 에 있는 사실만 쓴다. 맛 평가·별점·가격·리뷰·조리법은 언급 금지.
		4. exactStore 가 있는 요리는 그 가게에서 그대로 시킬 수 있다는 점을 먼저 말한다.
		   이때 요리는 brandName 으로 부르고, 가게는 exactStore 를 통째로 쓴다. 줄이지 않는다.
		5. candidateCount 가 0 인 요리는 반드시 언급하고, emptyReason 에 맞는 이유를 덧붙인다.
		6. candidateCount 가 1 이상인 요리는 그 개수를 숫자로 밝힌다. "많아요" 처럼 뭉개지 않는다.
		7. 후보 가게 이름은 나열하지 않는다. 가게 이름은 exactStore 만 쓴다.
		8. 숫자는 입력에 있는 값만 쓴다. 새로 만들지 않는다.

		[emptyReason]
		NO_NEARBY               반경 안에 가게가 없음
		DELIVERY_TIME_FILTERED  배달시간 조건에 다 걸림
		NO_SIMILAR_MENU         조건 안에서 비슷한 메뉴를 파는 가게를 못 찾음

		[출력]
		{"summary": "..."} 형태의 JSON 만 출력한다. 다른 텍스트를 붙이지 않는다.
		""";

	/**
	 * 규칙만으로는 "~해요" 가 지켜져도 문장 리듬이 매번 달라진다. 두 개를 붙여 톤을 고정한다.
	 * 하나는 브랜드 확정 + 0개 요리가 섞인 경우, 하나는 대안만 찾은 경우다.
	 */
	private static final List<Message> FEW_SHOT = List.of(
		new Message("user", """
			{"dishes":[{"name":"떡볶이","brandName":"엽기떡볶이","exactStore":"동대문엽기떡볶이 신촌점","candidateCount":5},\
			{"name":"마라탕","candidateCount":0,"emptyReason":"DELIVERY_TIME_FILTERED"}],\
			"preferences":{"maxDeliveryMin":30}}"""),
		new Message("assistant",
			"{\"summary\":\"영상 속 엽기떡볶이는 동대문엽기떡볶이 신촌점에서 그대로 시킬 수 있어요."
				+ " 마라탕은 배달 30분 안에 오는 곳이 없었어요\"}"),
		new Message("user", """
			{"dishes":[{"name":"후라이드치킨","candidateCount":3},\
			{"name":"감자튀김","candidateCount":4}],"preferences":{}}"""),
		new Message("assistant",
			"{\"summary\":\"영상에 나온 브랜드는 근처에 없지만, 후라이드치킨 3곳과 감자튀김 4곳을 찾았어요\"}"));

	/** 90자 제한이므로 이 이상은 필요 없다. */
	private static final int MAX_TOKENS = 150;

	/** 같은 입력에 문장이 크게 흔들리지 않을 정도로만 낮춘다. */
	private static final double TEMPERATURE = 0.3;

	/** 프롬프트를 고쳐야 한다는 신호. 넘겼다고 서버가 자르지는 않는다. */
	private static final int EXPECTED_MAX_CHARS = 90;

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
			log.info("분석 요약 ON  model={} baseUrl={} key={}", model, baseUrl, mask(apiKey));
		} else {
			log.warn("분석 요약 OFF (summary 는 항상 null). apiKey={} baseUrl={}", apiKey.isBlank() ? "없음" : "있음", baseUrl.isBlank() ? "없음" : baseUrl);
		}
	}

	/**
	 * @return 요약 한 줄. 설정이 없거나 호출·파싱이 실패하면 null.
	 *         호출자는 null 을 정상 흐름으로 처리해야 한다.
	 */
	public String summarize(SummaryRequest request) {
		if (!enabled) {
			log.debug("분석 요약이 꺼져 있어 건너뛴다. 기동 로그의 '분석 요약 OFF' 를 확인하라.");
			return null;
		}
		if (request == null || request.dishes() == null || request.dishes().isEmpty()) {
			log.debug("요약에 넘길 요리가 없다.");
			return null;
		}

		try {
			String payload = objectMapper.writeValueAsString(request);

			List<Message> messages = new java.util.ArrayList<>();
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
				log.warn("요약 응답이 비어 있다. summary 를 비우고 진행한다.");
				return null;
			}

			String summary = readSummary(content);
			if (summary != null && summary.length() > EXPECTED_MAX_CHARS) {
				// 자르지 않는다. 문장 중간에서 끊기면 안 쓰느니만 못하다. 프롬프트 튜닝 신호로만 남긴다.
				log.warn("요약이 {}자로 제한({}자)을 넘었다: {}", summary.length(), EXPECTED_MAX_CHARS, summary);
			}
			return summary;

		} catch (org.springframework.web.client.RestClientResponseException e) {
			// 403 이면 IAM 정책, 404 면 리전·모델명, 401 이면 키 자체를 의심한다.
			log.warn("분석 요약 호출 실패 status={} body={}",
				e.getStatusCode(), e.getResponseBodyAsString());
			return null;
		} catch (Exception e) {
			log.warn("분석 요약 생성 실패({}). summary 없이 진행한다: {}",
				e.getClass().getSimpleName(), e.getMessage());
			return null;
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
	 * 중괄호 구간만 잘라내 파싱한다. 그래도 실패하면 null 이다.
	 */
	private String readSummary(String content) {
		int from = content.indexOf('{');
		int to = content.lastIndexOf('}');
		if (from < 0 || to <= from) {
			log.warn("요약 응답에서 JSON 을 찾지 못했다: {}", content);
			return null;
		}
		try {
			SummaryBody body = objectMapper.readValue(content.substring(from, to + 1), SummaryBody.class);
			if (body == null || body.summary() == null || body.summary().isBlank()) {
				// JSON 은 맞는데 summary 키가 없다. 모델이 형식을 어겼거나 오류 본문이 왔다.
				log.warn("요약 응답에 summary 가 없다: {}", content);
				return null;
			}
			return body.summary().trim();
		} catch (Exception e) {
			log.warn("요약 JSON 파싱 실패: {}", content);
			return null;
		}
	}

	// ── 우리가 만들어 보내는 입력 ─────────────────────────────
	//    요약에 필요한 최소 사실만 담는다. AnalysisResponse 를 통째로 넘기면 가격·주소·score 까지
	//    들어가 모델이 그걸 쓰려 들고, "2만원짜리 치킨이 있어요" 같은 문장이 나와 카드와 어긋난다.

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SummaryRequest(List<DishSummary> dishes, Preferences preferences) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DishSummary(
		String name,
		String brandName,         // 영상에서 부른 브랜드. 없으면 null
		String exactStore,        // 브랜드 매칭 성공 시 지점명, 실패면 null
		int candidateCount,
		String emptyReason        // candidateCount == 0 일 때만
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Preferences(String maxSpiceLevel, Integer maxDeliveryMin, Boolean excludeMeat) {
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

	private record SummaryBody(String summary) {
	}
}
