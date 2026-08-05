package mukkeu.mukkeu.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

	@NotBlank(message = "이메일은 필수 입력값입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 320, message = "이메일은 320자를 넘을 수 없습니다.")
	String email,

	@NotBlank(message = "비밀번호 입력은 필수 입력값입니다.")
	@Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
	String password,

	@NotBlank(message = "닉네임은 필수 입력값입니다.")
	@Size(max = 50, message = "닉네임은 50자를 넘을 수 없습니다.")
	String nickName
) {
}
