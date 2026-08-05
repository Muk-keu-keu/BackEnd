package mukkeu.mukkeu.user.app;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.user.adapter.UserDetail;

/**
 * SecurityContext에서 현재 로그인한 사용자 정보를 꺼내온다.
 */
@Component
public class UserContextService {

	public Long getCurrentUserId() {
		return getCurrentUserDetail().getId();
	}

	public UserDetail getCurrentUserDetail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !(auth.getPrincipal() instanceof UserDetail userDetail)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}
		return userDetail;
	}
}
