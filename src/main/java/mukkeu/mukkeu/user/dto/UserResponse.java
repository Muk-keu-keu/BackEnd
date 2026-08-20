package mukkeu.mukkeu.user.dto;

import mukkeu.mukkeu.user.domain.Role;
import mukkeu.mukkeu.user.domain.User;

/**
 * 엔티티를 그대로 내보내지 않기 위한 응답 전용 DTO
 *
 * nickName 과 address 를 함께 내보낸다. 마이페이지가 이 둘을 화면에 그린다.
 * 닉네임은 PATCH /v1/users/me 로 바꿀 수 있는데 정작 읽을 곳이 없었다 —
 * 쓸 수는 있고 볼 수는 없는 비대칭이었다.
 *
 * password 는 담지 않는다. 회원 탈퇴가 비밀번호를 요구하지만 그건 사용자가
 * 직접 입력하는 값이지 서버가 돌려줄 값이 아니다.
 */
public record UserResponse(
	Long id,
	String email,
	Role role,
	String nickName,

	/** 배달 주소. 반경 5km 검색의 기준점이다. */
	String address
) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(),
			user.getNickName(), user.getAddress());
	}
}
