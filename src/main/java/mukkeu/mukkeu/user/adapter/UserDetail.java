package mukkeu.mukkeu.user.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import mukkeu.mukkeu.user.domain.Role;
import mukkeu.mukkeu.user.domain.User;
import lombok.Getter;

/**
 * Spring Security가 principal로 들고 다니는 인증 사용자 정보
 */
@Getter
public class UserDetail implements UserDetails {

	private final Long id;
	private final String email;
	private final String password;
	private final Role role;

	private UserDetail(Long id, String email, String password, Role role) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.role = role;
	}

	public static UserDetail from(User user) {
		return new UserDetail(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
	}

	/** 인가 검사에 쓰이는 권한 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.email;
	}
}
