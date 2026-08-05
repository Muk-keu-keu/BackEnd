package mukkeu.mukkeu.user.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.user.domain.RefreshToken;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface RefreshTokenRepository {

	RefreshToken save(RefreshToken refreshToken);

	/** 해당 유저의 가장 최근 refresh token */
	Optional<RefreshToken> findLatestByUserId(Long userId);

	List<RefreshToken> findAllByUserId(Long userId);

	void delete(RefreshToken refreshToken);

	void deleteAllByUserId(Long userId);
}
