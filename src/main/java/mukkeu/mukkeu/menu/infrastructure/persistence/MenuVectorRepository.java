package mukkeu.mukkeu.menu.infrastructure.persistence;

import java.io.StringReader;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import mukkeu.mukkeu.menu.domain.MenuMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 검색 전용. JPA 로는 VECTOR 타입을 다룰 수 없어 JDBC 로 직접 친다.
 *
 * ── 임베딩 호출을 두 쿼리로 나눈 이유 ─────────────────────────
 * 처음에는 한 문장 안에서 WITH 절로 질의 벡터를 만들고 menu 와 조인했다.
 * 의도는 "임베딩을 한 번만 만들어 재사용" 이었는데, 오라클 옵티마이저가 한 번만
 * 참조되는 WITH 절을 인라인으로 펼쳐 버려서 q.v 를 읽는 행마다 UTL_TO_EMBEDDING 이
 * 다시 실행됐다. 메뉴 150행이면 시카고 왕복이 150번, 응답이 3분이었다.
 *
 * /*+ MATERIALIZE * / 힌트로 막을 수는 있지만 힌트는 옵티마이저에 대한 "요청" 이라
 * 보장이 아니다. 시연 중에 실행계획이 바뀌어 3분이 되는 위험을 감수할 이유가 없다.
 *
 * 그래서 임베딩을 dual 만 읽는 별도 쿼리로 뺐다. 그 SQL 에는 테이블이 없으므로
 * "행마다" 라는 개념 자체가 성립하지 않는다. 호출 횟수가 계획과 무관하게 정확히 1회다.
 * DB 왕복이 한 번 늘지만 왕복은 수 ms 고 임베딩 왕복은 1200ms 라 비교가 안 된다.
 *
 * ── 나머지 설계 ──────────────────────────────────────────
 * 맵기와 고기 여부는 WHERE 가 답한다. embed_text 에는 맛만 넣었기 때문이다.
 * 'HOT' 을 임베딩에 섞으면 매운 떡볶이와 매운 짬뽕이 가까워져 유사도가 오염된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuVectorRepository {

	private static final String EMBED_PARAMS = """
		{ "provider"        : "ocigenai",
		  "credential_name" : "OCI_GENAI_CRED",
		  "url"             : "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/embedText",
		  "model"           : "cohere.embed-v4.0" }""";

	/**
	 * 질의문 → 벡터. 문자열로 받아 다음 쿼리에 바인드한다.
	 * CLOB 으로 받는 이유는 1536개 실수가 20KB 를 넘길 수 있어 VARCHAR2 한도에 걸리기 때문이다.
	 */
	private static final String EMBED_SQL = """
		SELECT FROM_VECTOR(
		         DBMS_VECTOR.UTL_TO_EMBEDDING(?, JSON('%s'))
		         RETURNING CLOB) AS v
		FROM dual""".formatted(EMBED_PARAMS);

	private final JdbcTemplate jdbcTemplate;

	/**
	 * 반경 안에서 이미 걸러진 가게들 중 질의문과 가까운 메뉴를 거리와 함께 돌려준다.
	 *
	 * @param maxSpiceRank 사용자가 허용한 맵기 상한(0/1/2). null 이면 제한 없음
	 */
	public List<MenuMatch> search(String queryText, List<Long> restaurantIds,
		Integer maxSpiceRank, boolean excludeMeat, int limit) {

		if (restaurantIds == null || restaurantIds.isEmpty() || queryText == null || queryText.isBlank()) {
			return List.of();
		}

		String vector = embedOnce(queryText);
		if (vector == null || vector.isBlank()) {
			// 임베딩을 못 만들면 검색할 기준이 없다. 빈 결과가 맞다.
			return List.of();
		}

		String inClause = restaurantIds.stream()
			.map(String::valueOf)
			.reduce((a, b) -> a + "," + b)
			.orElse("-1");

		StringBuilder sql = new StringBuilder("""
			SELECT m.menu_id,
			       VECTOR_DISTANCE(m.embedding, TO_VECTOR(?), COSINE) AS dist
			FROM   menu m
			WHERE  m.embedding IS NOT NULL
			  AND  m.restaurant_id IN (%s)
			""".formatted(inClause));

		if (maxSpiceRank != null) {
			sql.append(" AND m.spice_rank <= ").append(maxSpiceRank);
		}
		if (excludeMeat) {
			// NULL 은 자바 쪽 YnConverter 가 false(고기 없음)로 읽는다.
			// SQL 에서 = 'N' 만 쓰면 NULL 행이 UNKNOWN 으로 빠져 두 경로의 결과가 갈린다.
			sql.append(" AND (m.has_meat = 'N' OR m.has_meat IS NULL)");
		}
		// 거리 오름차순이다. 가까운 것이 위로 온다.
		sql.append(" ORDER BY dist FETCH FIRST ").append(limit).append(" ROWS ONLY");

		// 이 쿼리에는 임베딩 호출이 없다. 순수 계산이라 400행이어도 밀리초다.
		return jdbcTemplate.query(
			sql.toString(),
			ps -> ps.setCharacterStream(1, new StringReader(vector), vector.length()),
			(rs, rowNum) -> new MenuMatch(rs.getLong("menu_id"), rs.getDouble("dist")));
	}

	/** 임베딩 API 호출은 여기 한 곳뿐이고, 한 번 호출에 정확히 1회 나간다. */
	private String embedOnce(String queryText) {
		try {
			return jdbcTemplate.queryForObject(EMBED_SQL, String.class, queryText);
		} catch (Exception e) {
			log.warn("질의문 임베딩 실패. 벡터 검색을 건너뛴다: {}", e.getMessage());
			return null;
		}
	}
}
