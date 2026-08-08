package mukkeu.mukkeu.menu.domain;

/**
 * menu.options 컬럼의 JSON 배열 한 원소.
 *   [{"group":"사리 추가","name":"분모자","price":2000}, ...]
 *
 * group 은 없으면 null 이다. DB 에는 키 자체가 없고(JSON Schema 가 null 을 거절한다)
 * 자바로 읽을 때만 null 이 된다.
 */
public record MenuOption(String group, String name, Integer price) {
}
