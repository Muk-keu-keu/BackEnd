package mukkeu.mukkeu.post.infrastructure.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.post.domain.ImageStorage;
import lombok.extern.slf4j.Slf4j;

/**
 * OCI Object Storage 에 PAR(Pre-Authenticated Request) 로 올린다.
 *
 * SDK 를 쓰지 않는 이유는 PAR URL 자체가 인증이기 때문이다. SDK 를 붙이면 의존성이
 * 수십 MB 늘고 tenancy·user·fingerprint·private key 를 서버 환경변수로 또 넣어야 한다.
 * PAR 은 URL 하나에 PUT 한 번이면 끝난다.
 *
 * 그래서 par-url 은 비밀이다. 유출되면 누구나 이 버킷에 파일을 올릴 수 있다.
 * 읽기는 버킷 Visibility 가 Public 이라 public-base-url 로 그냥 열린다.
 */
@Slf4j
@Component
public class ObjectStorageImageStorage implements ImageStorage {

	private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp");
	private static final long MAX_BYTES = 5L * 1024 * 1024;   // 장당 5MB
	private static final String PREFIX = "posts/";

	private final RestClient restClient;
	private final String parUrl;
	private final String publicBaseUrl;

	public ObjectStorageImageStorage(
		@Value("${oci.storage.par-url}") String parUrl,
		@Value("${oci.storage.public-base-url}") String publicBaseUrl) {

		// 뒤에 오브젝트 이름을 붙일 것이므로 슬래시를 한 번만 남긴다.
		this.parUrl = withTrailingSlash(parUrl);
		this.publicBaseUrl = withTrailingSlash(publicBaseUrl);
		this.restClient = RestClient.create();
	}

	@Override
	public List<String> upload(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return List.of();
		}

		// 빈 파트는 버린다. Postman 에서 images 행을 켜 두고 파일을 안 고르거나,
		// 브라우저에서 빈 input 을 append 하면 크기 0 인 파트가 그대로 날아온다.
		// 그걸 400 으로 막으면 "사진 없이 글만" 이 안 되는 것처럼 보인다.
		List<MultipartFile> real = files.stream()
			.filter(f -> f != null && !f.isEmpty())
			.toList();
		if (real.isEmpty()) {
			log.info("업로드할 파일이 없다. images 파트가 비어 있었다");
			return List.of();
		}

		List<String> urls = new ArrayList<>();
		for (MultipartFile file : real) {
			urls.add(uploadOne(file));
		}
		log.info("이미지 {}장 업로드 완료", urls.size());
		return urls;
	}

	private String uploadOne(MultipartFile file) {
		String extension = extensionOf(file.getOriginalFilename());

		if (file.isEmpty() || file.getSize() > MAX_BYTES || !ALLOWED.contains(extension)) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}

		// 원본 파일명을 그대로 쓰지 않는다. 한글·공백·중복이 섞이면 URL 이 깨지고
		// 다른 사용자의 파일을 덮어쓸 수도 있다.
		String objectName = PREFIX + UUID.randomUUID() + "." + extension;

		try {
			restClient.put()
				.uri(parUrl + objectName)
				.contentType(MediaType.parseMediaType(
					file.getContentType() == null ? "application/octet-stream" : file.getContentType()))
				.body(file.getBytes())
				.retrieve()
				.toBodilessEntity();

		} catch (Exception e) {
			// 여기서 삼키면 사진 없는 게시글이 남는다. 글 작성 전체를 되돌린다.
			log.warn("이미지 업로드 실패: {}", e.getMessage());
			throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
		}

		return publicBaseUrl + objectName;
	}

	private static String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	private static String withTrailingSlash(String url) {
		return url.endsWith("/") ? url : url + "/";
	}
}
