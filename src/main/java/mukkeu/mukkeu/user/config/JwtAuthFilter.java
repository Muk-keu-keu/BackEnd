package mukkeu.mukkeu.user.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import mukkeu.mukkeu.user.adapter.UserDetail;
import mukkeu.mukkeu.user.app.JwtTokenProvider;
import mukkeu.mukkeu.user.app.UserService;
import mukkeu.mukkeu.user.dto.TokenBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 요청당 한 번 동작하며, Authorization 헤더의 access token으로 인증 정보를 채운다.
 * 토큰이 없거나 유효하지 않으면 인증하지 않고 그대로 흘려보내고,
 * 최종 차단 여부는 SecurityConfig의 인가 규칙이 결정한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserService userService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String accessToken = resolveToken(request);

		if (accessToken != null && jwtTokenProvider.validate(accessToken)) {
			try {
				TokenBody tokenBody = jwtTokenProvider.parseJwt(accessToken);
				UserDetail userDetail = userService.getDetails(tokenBody.userId());

				// 인증된 사용자임을 SecurityContext에 등록해 이후 인가 검사를 통과시킨다
				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (Exception e) {
				// 탈퇴 등으로 유저가 사라진 경우 : 인증하지 않고 통과시켜 401로 처리되게 한다
				log.debug("토큰은 유효하지만 인증 정보를 만들지 못했습니다: {}", e.getMessage());
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	/** Authorization 헤더에서 access token 추출 */
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}
		return null;
	}
}
