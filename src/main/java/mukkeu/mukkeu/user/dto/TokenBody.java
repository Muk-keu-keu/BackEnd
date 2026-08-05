package mukkeu.mukkeu.user.dto;

import mukkeu.mukkeu.user.domain.Role;

/**
 * JWT에서 꺼낸 사용자 정보
 */
public record TokenBody(Long userId, String email, Role role) {
}
