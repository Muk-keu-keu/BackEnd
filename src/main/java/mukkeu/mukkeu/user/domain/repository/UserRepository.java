package mukkeu.mukkeu.user.domain.repository;

import java.util.List;
import java.util.Optional;

import mukkeu.mukkeu.user.domain.User;

/**
 * 도메인 계층의 포트. JPA 등 영속 기술에 의존하지 않는다.
 */
public interface UserRepository {

	User save(User user);

	Optional<User> findById(Long id);

	/** 목록에서 작성자 닉네임을 한 번에 읽는다. 글마다 조회하면 N+1 이다. */
	List<User> findAllByIdIn(List<Long> ids);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	void delete(User user);
}
