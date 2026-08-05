package mukkeu.mukkeu.menu.domain;

/**
 * 3단계로 고정한다. 가게마다 5단계·3단계로 제각각인 것을 맞추려던
 * rank·그룹 규칙을 전부 버리고 이 셋만 쓴다.
 *
 * menu.spice_level 의 뜻은 "이 메뉴를 주문할 때 도달 가능한 가장 순한 맵기".
 * 필터가 묻는 것이 "이거 맵나?" 가 아니라 "내가 먹을 수 있나?" 이기 때문이다.
 */
public enum SpiceLevel {

	NONE(0), MEDIUM(1), HOT(2);

	private final int rank;

	SpiceLevel(int rank) {
		this.rank = rank;
	}

	public int getRank() {
		return rank;
	}

	/** 사용자가 허용한 상한을 넘지 않는가 */
	public boolean isWithin(SpiceLevel max) {
		return max == null || this.rank <= max.rank;
	}
}
