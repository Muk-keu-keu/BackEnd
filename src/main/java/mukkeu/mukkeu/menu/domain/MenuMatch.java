package mukkeu.mukkeu.menu.domain;

/**
 * 벡터 검색 결과 한 줄. 엔티티가 embedding 을 담지 못하므로 따로 받아 온다.
 * distance 는 코사인 거리(0에 가까울수록 유사)라 유사도로 뒤집어 쓴다.
 * distance 는 코사인 "거리" 라 방향이 뒤집혀 있다. 0 이면 같은 뜻,
 * 1 이면 무관, 2 면 정반대다. 즉 작을수록 좋은 결과다.
 *
 * 그대로 내보내면 comboScore 0.08 이 최고점인데 낮아 보이고,
 * "클수록 좋음" 인 optionMatchRatio 와 한 식에서 더할 수도 없다.
 * 그래서 여기서 한 번만 뒤집고, 이후 모든 점수는 클수록 좋음으로 통일한다.
 */
public record MenuMatch(Long menuId, Double distance) {

	public double similarity() {
		return 1.0 - distance;
	}
}
