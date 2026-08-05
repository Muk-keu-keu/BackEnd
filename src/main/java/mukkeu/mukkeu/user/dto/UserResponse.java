package mukkeu.mukkeu.user.dto;

import mukkeu.mukkeu.user.domain.Role;
import mukkeu.mukkeu.user.domain.User;

/**
 * 엔티티를 그대로 내보내지 않기 위한 응답 전용 DTO
 */
public record UserResponse(Long id, String email, Role role) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole());
	}
}
