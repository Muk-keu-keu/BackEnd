package mukkeu.mukkeu.global.exception.domain;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

	/*
	 * Commons : 공통 예외 처리
	 */
	// 400
	INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST, "요청 데이터가 올바르지 않습니다. 입력 데이터를 확인해 주세요."),

	// 401
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요한 요청입니다. 로그인 해주세요."),

	// 403
	FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다."),

	// 404
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),
	// 남의 주문도 여기로 온다. 403 을 주면 "그 번호의 주문이 존재한다" 는 사실이 새어 나간다.
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),

	// 500
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다."),

	/*
	 * Auth : 로그인 / 인증 관련 예외 처리
	 */
	// 401
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
	INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
	TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "서버에 저장된 토큰과 일치하지 않습니다."),
	AUTH_HEADER_MISSING(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 누락되었습니다."),

	// 404
	LOGIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "로그인 정보와 일치하는 사용자가 존재하지 않습니다."),

	// 409
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),

	/*
	 * 요기족보
	 */
	// 400
	INVALID_IMAGE(HttpStatus.BAD_REQUEST, "이미지는 jpg/png/webp 형식의 5MB 이하만 올릴 수 있습니다."),
	TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "이미지는 최대 5장까지 올릴 수 있습니다."),

	// 409
	POST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 이 주문으로 작성한 글이 있습니다."),

	// 500
	IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
