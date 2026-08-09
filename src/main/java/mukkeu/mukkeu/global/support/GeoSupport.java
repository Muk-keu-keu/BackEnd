package mukkeu.mukkeu.global.support;

/**
 * 반경 계산.
 *
 * 박스(사각형)로 먼저 거르고 원으로 다듬는다. WHERE 절에 하버사인을 쓰면
 * 컬럼에 함수가 걸려 ix_rest_geo 인덱스를 못 타고 전체 스캔이 된다.
 * 박스로 수십 개까지 줄인 뒤 자바에서 정확한 거리를 재는 편이 훨씬 싸다.
 */
public final class GeoSupport {

	private static final double EARTH_RADIUS_KM = 6371.0;
	private static final double KM_PER_LAT_DEGREE = 111.0;

	private GeoSupport() {
	}

	/** 위도 방향 반경(도). 위도는 어디서나 1도가 약 111km 로 일정하다. */
	public static double latDelta(double radiusKm) {
		return radiusKm / KM_PER_LAT_DEGREE;
	}

	/** 경도 방향 반경(도). 극으로 갈수록 좁아지므로 cos(위도)로 나눈다. */
	public static double lngDelta(double radiusKm, double lat) {
		return radiusKm / (KM_PER_LAT_DEGREE * Math.cos(Math.toRadians(lat)));
	}

	/** 두 좌표 사이 직선거리(km) */
	public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
	}
}
