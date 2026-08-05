package mukkeu.mukkeu.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mukkeu.mukkeu.user.domain.RefreshToken;
import mukkeu.mukkeu.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

	private final RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return refreshTokenJpaRepository.save(refreshToken);
	}

	@Override
	public Optional<RefreshToken> findLatestByUserId(Long userId) {
		return refreshTokenJpaRepository.findTop1ByUserIdOrderByIdDesc(userId);
	}

	@Override
	public List<RefreshToken> findAllByUserId(Long userId) {
		return refreshTokenJpaRepository.findAllByUserId(userId);
	}

	@Override
	public void delete(RefreshToken refreshToken) {
		refreshTokenJpaRepository.delete(refreshToken);
	}

	@Override
	public void deleteAllByUserId(Long userId) {
		refreshTokenJpaRepository.deleteAllByUserId(userId);
	}
}
