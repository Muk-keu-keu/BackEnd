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

	/**
	 * 비로그인도 허용하는 조회에서 쓴다. 요기족보 홈·단건이 그렇다.
	 *
	 * 로그인했으면 "내가 좋아요를 눌렀나" 를 채워 주고, 아니면 전부 false 로 내려간다.
	 * 이 경우 인증이 없는 것은 오류가 아니라 정상 상태라 예외를 던지지 않는다.
	 */
	public Long getCurrentUserIdOrNull() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !(auth.getPrincipal() instanceof UserDetail userDetail)) {
			return null;
		}
		return userDetail.getId();
	}

	public UserDetail getCurrentUserDetail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !(auth.getPrincipal() instanceof UserDetail userDetail)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}
		return userDetail;
	}
}
