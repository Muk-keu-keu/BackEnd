package mukkeu.mukkeu.user.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 1000, nullable = false)
	private String refreshToken;

	// 연관관계(@ManyToOne) 없이 유저 식별자만 들고 간다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Builder
	public RefreshToken(String refreshToken, Long userId) {
		this.refreshToken = refreshToken;
		this.userId = userId;
	}

	public void newSetRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
