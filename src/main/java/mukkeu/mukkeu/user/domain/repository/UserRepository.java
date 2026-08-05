package mukkeu.mukkeu.user.domain.repository;

import java.util.Optional;

import mukkeu.mukkeu.user.domain.User;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface UserRepository {

	User save(User user);

	Optional<User> findById(Long id);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	void delete(User user);
}
