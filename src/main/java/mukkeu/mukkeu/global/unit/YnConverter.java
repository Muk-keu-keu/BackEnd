package mukkeu.mukkeu.global.unit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CHAR(1) 'Y'/'N' <-> boolean
 * 오라클 스키마가 CHAR(1) 을 쓰므로 자바 쪽만 boolean 으로 다룬다.
 */
@Converter
public class YnConverter implements AttributeConverter<Boolean, String> {

	@Override
	public String convertToDatabaseColumn(Boolean value) {
		return Boolean.TRUE.equals(value) ? "Y" : "N";
	}

	@Override
	public Boolean convertToEntityAttribute(String dbData) {
		return "Y".equalsIgnoreCase(dbData);
	}
}
