package mukkeu.mukkeu.post.domain;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 이미지 저장소. 구현은 infrastructure 에 둔다.
 *
 * 인터페이스로 빼는 이유는 저장 위치가 바뀔 수 있어서다. 지금은 OCI Object Storage 지만
 * 로컬 폴더나 S3 로 갈아끼워도 PostService 는 그대로다.
 */
public interface ImageStorage {

	/**
	 * 올리고 공개 URL 을 돌려준다. 순서는 넣은 순서를 지킨다.
	 *
	 * 실패하면 예외를 던진다. 조용히 건너뛰면 이미지가 비거나 순서가 밀린 게시글이
	 * 남는데, 사용자는 왜 사진이 사라졌는지 알 수 없다. 글 작성 전체를 롤백시키는 편이 낫다.
	 */
	List<String> upload(List<MultipartFile> files);
}
