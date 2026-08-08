package mukkeu.mukkeu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNickNameRequest(

	@NotBlank(message = "닉네임은 필수 입력값입니다.")
	@Size(max = 50, message = "닉네임은 50자를 넘을 수 없습니다.")
	String nickName
) {
}
