package mukkeu.mukkeu.user.domain;

import mukkeu.mukkeu.global.unit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends BaseEntity {

	@Id
	@Column(name = "user_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 320, nullable = false)
	private String email;

	// BCrypt 등 인코딩 접두어까지 담기므로 넉넉하게 잡는다
	@Column(length = 254, nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private Role role;

	private String address;

	private Double lat;

	private Double lng;

	private String nickName;

	public static User create(String email, String password, String nickName){
		User user = new User();
		user.email=email;
		user.password=password;
		user.address="강남구 영동대로 517 아셈타워 15층";
		user.lat=37.5130;
		user.lng=127.059;
		user.role=Role.USER;
		user.nickName=nickName;
		return user;
	}
}
