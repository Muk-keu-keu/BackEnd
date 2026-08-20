package mukkeu.mukkeu.credit.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 사람이 한 가게에 가진 채움 포인트 잔액.
 *
 * 복합 PK 가 "사람×가게에 잔액은 하나" 를 보장한다. 같은 조합의 행이 둘 생기면
 * 어느 쪽을 읽느냐에 따라 결제액이 달라지는데, 그건 돈이 틀리는 버그라 DB 로 막는다.
 *
 * ★ 이력을 여기 두지 않는다. orders.used_point / earned_point 가 이력이다.
 *   orders 에는 이미 user_id, restaurant_id, checkout_id, created_at 이 있어서
 *   이력 테이블을 따로 두면 같은 사실을 두 군데 적게 되고, 둘이 어긋나면
 *   어느 쪽이 맞는지 판단할 근거가 없다.
 *
 * ★ 잔액은 절대 음수가 되지 않는다. 차감은 use() 한 곳에서만 일어나고,
 *   거기서 잔액을 넘는 요청을 막는다. DB 에도 CHECK 제약을 함께 걸어 둔다.
 */
@Entity
@Getter
@Table(name = "user_store_credit")
@IdClass(UserStoreCreditId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStoreCredit {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Id
	@Column(name = "restaurant_id")
	private Long restaurantId;

	@Column(nullable = false)
	private Integer balance;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public UserStoreCredit(Long userId, Long restaurantId) {
		this.userId = userId;
		this.restaurantId = restaurantId;
		this.balance = 0;
		this.updatedAt = LocalDateTime.now();
	}

	/**
	 * 포인트를 쓴다.
	 *
	 * @throws IllegalArgumentException 잔액을 넘겨 쓰려 할 때. 호출자가 이미
	 *         min(balance, ...) 으로 잘라서 넘기므로 여기까지 오면 계산이 틀린 것이다.
	 *         조용히 0 으로 깎으면 사용자는 낸 줄 아는데 잔액이 그대로 남는다.
	 */
	public void use(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("포인트 사용액은 음수일 수 없다: " + amount);
		}
		if (amount > this.balance) {
			throw new IllegalArgumentException(
				"잔액을 넘겨 쓸 수 없다. 잔액=" + this.balance + " 요청=" + amount);
		}
		this.balance -= amount;
		this.updatedAt = LocalDateTime.now();
	}

	/** 최소주문 미달분을 적립한다. */
	public void earn(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("포인트 적립액은 음수일 수 없다: " + amount);
		}
		this.balance += amount;
		this.updatedAt = LocalDateTime.now();
	}
}
