package mukkeu.mukkeu.menu.infrastructure.persistence;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 벡터 검색 전용. JPA 로는 VECTOR 타입을 다룰 수 없어 JDBC 로 직접 친다.
 *
 * ★ UTL_TO_EMBEDDING 을 ORDER BY 안에 직접 넣으면 행마다 임베딩 API 를
 *   호출한다. WITH 절로 한 번만 만들어 두고 그 값을 참조해야 한다.
 */
@Component
@RequiredArgsConstructor
public class MenuVectorRepository {

	private static final String EMBED_PARAMS = """
		{ "provider"        : "ocigenai",
		  "credential_name" : "OCI_GENAI_CRED",
		  "url"             : "https://inference.generativeai.us-chicago-1.oci.oraclecloud.com/20231130/actions/embedText",
		  "model"           : "cohere.embed-v4.0" }
		""";

	private final JdbcTemplate jdbcTemplate;

	/**
	 * 반경 안에서 이미 걸러진 가게들 중 질의문과 가까운 메뉴 id 를 돌려준다.
	 * 맵기·고기 조건은 벡터가 아니라 WHERE 가 답한다 — 맛만 임베딩하기 때문이다.
	 */
	public List<Long> searchMenuIds(String queryText, List<Long> restaurantIds,
		Integer maxSpiceRank, boolean excludeMeat, int limit) {

		if (restaurantIds == null || restaurantIds.isEmpty()) {
			return List.of();
		}

		String inClause = restaurantIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("-1");

		StringBuilder sql = new StringBuilder("""
			WITH q AS (
			  SELECT DBMS_VECTOR.UTL_TO_EMBEDDING(?, JSON('%s')) AS v FROM dual
			)
			SELECT m.menu_id
			FROM   menu m, q
			WHERE  m.embedding IS NOT NULL
			  AND  m.restaurant_id IN (%s)
			""".formatted(EMBED_PARAMS, inClause));

		if (maxSpiceRank != null) {
			sql.append(" AND m.spice_rank <= ").append(maxSpiceRank);
		}
		if (excludeMeat) {
			sql.append(" AND m.has_meat = 'N'");
		}
		sql.append(" ORDER BY VECTOR_DISTANCE(m.embedding, q.v, COSINE) FETCH FIRST ? ROWS ONLY");

		return jdbcTemplate.queryForList(sql.toString(), Long.class, queryText, limit);
	}
}
