package mukkeu.mukkeu.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.user.domain.RefreshToken;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findTop1ByUserIdOrderByIdDesc(Long userId);

	List<RefreshToken> findAllByUserId(Long userId);

	void deleteAllByUserId(Long userId);
}
