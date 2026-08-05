package mukkeu.mukkeu.menu.domain;

import mukkeu.mukkeu.global.unit.YnConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴. 벡터 검색의 단위다.
 *
 * ★ embedding(VECTOR(1536)) 컬럼은 매핑하지 않는다.
 *   JPA 표준 타입이 아니라 엔티티에 담을 수 없다. 유사도 검색은
 *   VECTOR_DISTANCE 를 쓰는 네이티브 쿼리로 menu_id 만 받아온 뒤
 *   그 id 로 이 엔티티를 조회한다.
 */
@Entity
@Getter
@Table(name = "menu")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

	/** restaurant_id * 1000 + 순번. 시드에서 직접 매긴다. */
	@Id
	@Column(name = "menu_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 연관관계 없이 식별자만 들고 간다. */
	@Column(name = "restaurant_id", nullable = false)
	private Long restaurantId;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "menu_type", length = 10)
	private MenuType menuType;

	/** 줄임말·별칭. '황올, 고바사' — 먹방 캡션 매칭률을 좌우한다. */
	@Column(length = 300)
	private String aliases;

	/** 맛·식감 한 줄. 임베딩되는 원문이다. */
	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private Integer price;

	@Enumerated(EnumType.STRING)
	@Column(name = "spice_level", nullable = false, length = 10)
	private SpiceLevel spiceLevel;

	/** 주문할 때 맵기를 고를 수 있는 메뉴인가. 'Y' 면 spice_level 은 NONE 이다. */
	@Convert(converter = YnConverter.class)
	@Column(name = "spice_adjustable", nullable = false, length = 1)
	private Boolean spiceAdjustable;

	/** 가상 컬럼. DB 가 계산하므로 읽기 전용이다. */
	@Column(name = "spice_rank", insertable = false, updatable = false)
	private Integer spiceRank;

	@Convert(converter = YnConverter.class)
	@Column(name = "is_healthy", length = 1)
	private Boolean healthy;

	@Convert(converter = YnConverter.class)
	@Column(name = "has_meat", length = 1)
	private Boolean hasMeat;

	@Column(name = "taste_tags", length = 300)
	private String tasteTags;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * 주문 옵션. JSON 배열 문자열이다.
	 *   [{"group":"사리 추가","name":"분모자","price":2000}, ...]
	 * 없으면 NULL 이다(빈 배열이 아니다). 파싱은 서비스 계층에서 Jackson 으로.
	 */
	@Column(length = 4000)
	private String options;

	/** 실제로 임베딩된 원문. 결과가 이상할 때 눈으로 확인한다. */
	@Column(name = "embed_text", length = 1000)
	private String embedText;
}
