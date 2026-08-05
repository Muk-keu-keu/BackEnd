package mukkeu.mukkeu.global.exception.handler;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.global.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 로직에서 발생한 예외 처리 (ErrorCode 기반)
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, path(request)));
	}

	/**
	 * @Valid 검증 실패 (필드 메시지를 그대로 내려준다)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
		HttpServletRequest request) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getDefaultMessage())
			.orElse(ErrorCode.INVALID_REQUEST_DATA.getMessage());

		ErrorCode code = ErrorCode.INVALID_REQUEST_DATA;
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.builder()
			.status(code.getStatus().value())
			.code(code.name())
			.message(message)
			.path(path(request))
			.build());
	}

	/**
	 * 이메일 unique 제약 위반 (동시 회원가입 등)
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e,
		HttpServletRequest request) {
		log.warn("무결성 제약 위반: {}", e.getMessage());
		ErrorCode code = ErrorCode.EMAIL_ALREADY_EXISTS;
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, path(request)));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
		ErrorCode code = ErrorCode.FORBIDDEN_ACCESS;
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, path(request)));
	}

	private String path(HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}
}
