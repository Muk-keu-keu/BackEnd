package mukkeu.mukkeu.user.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.global.exception.dto.ErrorResponse;
import mukkeu.mukkeu.user.app.JwtTokenProvider;
import mukkeu.mukkeu.user.app.UserService;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	/** 허용할 클라이언트 origin (앱은 CORS 대상이 아니지만 웹/로컬 테스트용으로 열어둔다) */
	@Value("${custom.cors.allowed-origins}")
	private String[] allowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtTokenProvider jwtTokenProvider,
		UserService userService,
		ObjectMapper objectMapper) throws Exception {

		JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider, userService);

		return http
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			// JWT + 무상태라 세션 기반 CSRF 토큰은 사용하지 않는다
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			.authorizeHttpRequests(auth -> auth
				// reissue는 access token이 이미 만료된 상태로 호출되므로 열어둔다
				.requestMatchers("/v1/users/signup", "/v1/users/login", "/v1/users/reissue-token").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().authenticated())

			.exceptionHandling(handler -> handler
				.authenticationEntryPoint((request, response, e) ->
					writeError(response, objectMapper, ErrorCode.AUTHENTICATION_REQUIRED,
						request.getMethod() + " " + request.getRequestURI()))
				.accessDeniedHandler((request, response, e) ->
					writeError(response, objectMapper, ErrorCode.FORBIDDEN_ACCESS,
						request.getMethod() + " " + request.getRequestURI())))

			.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	private void writeError(HttpServletResponse response, ObjectMapper objectMapper,
		ErrorCode errorCode, String path) throws java.io.IOException {

		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, path));
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList(allowedOrigins));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
