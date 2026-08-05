package mukkeu.mukkeu.user.app;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import mukkeu.mukkeu.user.domain.Role;
import mukkeu.mukkeu.user.dto.TokenBody;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰을 발급하고 검증하는 제공자
 */
@Slf4j
@Component
public class JwtTokenProvider {

	/** JWT 서명 키. HS256 이라 32바이트 이상이어야 한다. */
	private final SecretKey secretKey;

	/** 밀리초 단위 만료 시간 */
	private final long accessExpMillis;
	private final long refreshExpMillis;

	public JwtTokenProvider(
		@Value("${custom.jwt.secrets.appkey}") String appkey,
		@Value("${custom.jwt.exp-time.access}") long accessExpMillis,
		@Value("${custom.jwt.exp-time.refresh}") long refreshExpMillis) {

		// 키는 부팅 때 한 번만 만든다. 토큰마다 다시 만들 이유가 없다.
		// 값이 비었거나 32바이트 미만이면 여기서 WeakKeyException 이 나 앱이 안 뜬다.
		this.secretKey = Keys.hmacShaKeyFor(appkey.getBytes(StandardCharsets.UTF_8));
		this.accessExpMillis = accessExpMillis;
		this.refreshExpMillis = refreshExpMillis;
	}

	public String issueAccessToken(Long id, Role role, String email) {
		return issueToken(id, role, email, accessExpMillis);
	}

	public String issueRefreshToken(Long id, Role role, String email) {
		return issueToken(id, role, email, refreshExpMillis);
	}

	private String issueToken(Long id, Role role, String email, long expMillis) {
		Date now = new Date();
		return Jwts.builder()
			.subject(id.toString())
			.claim("role", role.name())
			.claim("email", email)
			.issuedAt(now)
			.expiration(new Date(now.getTime() + expMillis))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}

	/**
	 * 형식 / 서명 / 만료 여부만 검사한다. (access, refresh 구분 없음)
	 */
	public boolean validate(String token) {
		try {
			JwtParser jwtParser = Jwts.parser()
				.verifyWith(secretKey)
				.build();

			// 서명이 변조되었는지, 만료 시간이 지났는지를 여기서 판단한다
			jwtParser.parseSignedClaims(token);
			return true;

		} catch (ExpiredJwtException e) {
			log.debug("토큰 만료됨: {} - {}", maskToken(token), e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.warn("지원되지 않는 토큰 형식: {} - {}", maskToken(token), e.getMessage());
		} catch (MalformedJwtException e) {
			log.warn("구조가 잘못된 토큰: {} - {}", maskToken(token), e.getMessage());
		} catch (JwtException e) {
			log.warn("잘못된 토큰 입력됨: {} - {}", maskToken(token), e.getMessage());
		} catch (Exception e) {
			log.error("알 수 없는 오류: {} - {}", maskToken(token), e.getMessage(), e);
		}
		return false;
	}

	/**
	 * JWT에서 사용자 정보 꺼내기
	 */
	public TokenBody parseJwt(String token) {
		Jws<Claims> parsed = Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token);

		Claims payload = parsed.getPayload();
		Long userId = Long.parseLong(payload.getSubject());
		Role role = Role.valueOf(payload.get("role", String.class));
		String email = payload.get("email", String.class);

		return new TokenBody(userId, email, role);
	}

	public Date getExpiration(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getExpiration();
	}

	/** 로그 출력용 마스킹 */
	public String maskToken(String token) {
		if (token == null || token.length() < 10) {
			return "(short token)";
		}
		return token.substring(0, 10) + "...(masked)";
	}
}
