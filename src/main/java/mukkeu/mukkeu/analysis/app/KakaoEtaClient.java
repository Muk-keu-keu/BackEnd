package mukkeu.mukkeu.analysis.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import mukkeu.mukkeu.restaurant.domain.Restaurant;
import lombok.extern.slf4j.Slf4j;

/**
 * 카카오모빌리티 다중 목적지 길찾기. 사용자 좌표에서 각 가게까지의 이동시간을 잰다.
 *
 * 가게 테이블의 delivery_min 은 가게가 스스로 적어 둔 고정값이라 사용자 위치를 모른다.
 * 0.08km 인 집과 4.9km 인 집이 똑같이 40분으로 나오면 "다른 가게 보기" 가 의미를 잃는다.
 *
 * 실패하면 빈 결과를 돌려준다. 호출자가 delivery_min 으로 되돌아가면 되고,
 * 길찾기가 안 된다고 검색 전체가 죽으면 안 된다.
 *
 * 좌표 표기에 주의한다. 카카오는 x 가 경도(lng), y 가 위도(lat) 다. 위경도 순서와 반대다.
 */
@Slf4j
@Component
public class KakaoEtaClient {

	/** 한 번에 보낼 수 있는 목적지 수. 카카오 제한이다. */
	private static final int MAX_DESTINATIONS = 30;

	/** 카카오가 허용하는 최대 반경(m). 우리 검색 반경 5km 보다 넉넉하다. */
	private static final int RADIUS_METERS = 10000;

	private final RestClient restClient;
	private final String apiKey;

	public KakaoEtaClient(
		@Value("${kakao.api.key}") String apiKey,
		@Value("${kakao.api.base-url}") String baseUrl) {

		this.apiKey = apiKey;
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
	}

	/**
	 * @return 가게 id → 이동시간(분). 길을 못 찾은 가게는 아예 담기지 않는다.
	 *         호출 전체가 실패해도 빈 맵이지 예외가 아니다.
	 */
	public Map<Long, Integer> travelMinutes(double originLat, double originLng, List<Restaurant> stores) {

		Map<Long, Integer> result = new HashMap<>();
		if (stores == null || stores.isEmpty()) {
			return result;
		}

		List<Restaurant> targets = stores.stream()
			.filter(r -> r.getLat() != null && r.getLng() != null)
			.toList();

		for (int from = 0; from < targets.size(); from += MAX_DESTINATIONS) {
			int to = Math.min(from + MAX_DESTINATIONS, targets.size());
			result.putAll(callOnce(originLat, originLng, targets.subList(from, to)));
		}
		return result;
	}

	private Map<Long, Integer> callOnce(double originLat, double originLng, List<Restaurant> chunk) {

		List<Destination> destinations = new ArrayList<>();
		for (Restaurant store : chunk) {
			destinations.add(new Destination(store.getLng(), store.getLat(), String.valueOf(store.getId())));
		}

		try {
			DirectionsResponse response = restClient.post()
				.uri("/v1/destinations/directions")
				.header("Authorization", "KakaoAK " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new DirectionsRequest(
					new Origin(originLng, originLat), destinations, RADIUS_METERS))
				.retrieve()
				.body(DirectionsResponse.class);

			if (response == null || response.routes() == null) {
				return Map.of();
			}

			Map<Long, Integer> minutes = new HashMap<>();
			for (Route route : response.routes()) {
				// result_code 0 만 성공이다. 섬이나 도로가 없는 좌표는 여기서 걸러진다.
				if (route.resultCode() != 0 || route.summary() == null || route.key() == null) {
					continue;
				}
				try {
					// duration 은 초 단위다. 올림해서 분으로 바꾼다.
					minutes.put(Long.valueOf(route.key()),
						(int)Math.ceil(route.summary().duration() / 60.0));
				} catch (NumberFormatException ignored) {
					// key 는 우리가 넣은 가게 id 다. 여기 오면 카카오가 값을 바꾼 것이다.
				}
			}
			return minutes;

		} catch (Exception e) {
			log.warn("카카오 길찾기 실패. delivery_min 으로 대체한다: {}", e.getMessage());
			return Map.of();
		}
	}

	// ── 카카오 요청/응답 ─────────────────────────────────────
	//    필드명이 snake_case 라 @JsonProperty 로 맞춘다.
	//    (Jackson 3 에서도 애노테이션 패키지는 com.fasterxml 그대로다)

	private record Origin(double x, double y) {
	}

	private record Destination(double x, double y, String key) {
	}

	private record DirectionsRequest(Origin origin, List<Destination> destinations, int radius) {
	}

	private record DirectionsResponse(@JsonProperty("routes") List<Route> routes) {
	}

	private record Route(
		@JsonProperty("result_code") int resultCode,
		@JsonProperty("key") String key,
		@JsonProperty("summary") Summary summary
	) {
	}

	private record Summary(
		@JsonProperty("distance") int distance,   // m
		@JsonProperty("duration") int duration    // 초
	) {
	}
}
