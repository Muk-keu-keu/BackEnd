package mukkeu.mukkeu.credit.domain;

import java.io.Serializable;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** user_store_credit 의 복합 기본키 (user_id, restaurant_id) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStoreCreditId implements Serializable {

	private Long userId;
	private Long restaurantId;

	public UserStoreCreditId(Long userId, Long restaurantId) {
		this.userId = userId;
		this.restaurantId = restaurantId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserStoreCreditId other)) {
			return false;
		}
		return Objects.equals(userId, other.userId)
			&& Objects.equals(restaurantId, other.restaurantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, restaurantId);
	}
}
