package mukkeu.mukkeu.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mukkeu.mukkeu.user.domain.User;

public interface UserJpaRepository extends JpaRepository<User, Long> {

	List<User> findAllByIdIn(List<Long> ids);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
