package mukkeu.mukkeu.global.exception.dto;

import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import lombok.Builder;

@Builder
public record ErrorResponse(int status, String code, String message, String path) {

	public static ErrorResponse of(ErrorCode errorCode, String path) {
		return ErrorResponse.builder()
			.status(errorCode.getStatus().value())
			.code(errorCode.name())
			.message(errorCode.getMessage())
			.path(path)
			.build();
	}
}
