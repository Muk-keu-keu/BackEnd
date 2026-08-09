package mukkeu.mukkeu.post.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mukkeu.mukkeu.post.app.PostService;
import mukkeu.mukkeu.post.dto.CommentRequest;
import mukkeu.mukkeu.post.dto.CommentResponse;
import mukkeu.mukkeu.post.dto.LikeResponse;
import mukkeu.mukkeu.post.dto.PostCreateRequest;
import mukkeu.mukkeu.post.dto.PostCreateResponse;
import mukkeu.mukkeu.post.dto.PostDetailResponse;
import mukkeu.mukkeu.post.dto.PostListResponse;
import mukkeu.mukkeu.post.dto.PostUpdateRequest;
import mukkeu.mukkeu.user.app.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 요기족보. 목록·단건은 비로그인도 볼 수 있고, 나머지는 로그인이 필요하다.
 */
@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final UserContextService userContextService;

	/**
	 * 글과 이미지를 한 요청으로 받는다. 업로드를 따로 떼면 사용자가 글을 안 쓰고
	 * 이탈했을 때 주인 없는 파일이 버킷에 남는다.
	 *
	 * 글 필드는 JSON 파트가 아니라 평범한 form-data 텍스트 필드다.
	 *   checkoutId, title, body  각각 한 줄
	 *   images                   파일 (여러 장이면 같은 이름으로 반복)
	 *
	 * 예전에는 data 파트에 JSON 을 담았는데, 그러면 그 파트에 Content-Type:
	 * application/json 을 직접 붙여야 한다. 안 붙이면 스프링이 octet-stream 으로
	 * 보고 415 를 낸다. 그런데 파트별 Content-Type 은 최신 Postman 에서 지정할 수
	 * 없고, 브라우저 FormData 도 문자열을 넣으면 타입을 안 붙인다. 프론트가 매번
	 * Blob 으로 감싸야 하는 구조라 평평한 폼 필드로 바꿨다.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PostCreateResponse create(
		@ModelAttribute @Valid PostCreateRequest data,
		@RequestPart(value = "images", required = false) List<MultipartFile> images) {

		return postService.create(userContextService.getCurrentUserId(), data, images);
	}

	/** 비로그인 허용. 토큰이 없으면 liked 가 전부 false 로 내려간다. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public PostListResponse getList(
		@RequestParam(required = false, defaultValue = "LATEST") String sort,
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false) Integer size) {

		return postService.getList(userContextService.getCurrentUserIdOrNull(), sort, cursor, size);
	}

	/** 비로그인 허용. order 에 글쓴이의 결제 내역이 통째로 들어간다(따라담기용). */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{postId}")
	public PostDetailResponse getDetail(@PathVariable Long postId) {
		return postService.getDetail(userContextService.getCurrentUserIdOrNull(), postId);
	}

	/**
	 * 본인 글만 수정. 이미지는 최종 상태를 통째로 보낸다.
	 *   data.keepImageUrls  남길 URL 을 원하는 순서로 (빠진 것은 삭제, 순서가 곧 재배열)
	 *   images              새로 추가할 파일. keepImageUrls 뒤에 붙는다
	 */
	@ResponseStatus(HttpStatus.OK)
	@PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PostDetailResponse update(
		@PathVariable Long postId,
		@ModelAttribute @Valid PostUpdateRequest data,
		@RequestPart(value = "images", required = false) List<MultipartFile> images) {

		return postService.update(userContextService.getCurrentUserId(), postId, data, images);
	}

	/** 본인 글만 삭제. 좋아요·댓글·이미지도 같이 사라진다. */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{postId}")
	public void delete(@PathVariable Long postId) {
		postService.delete(userContextService.getCurrentUserId(), postId);
	}

	/** 이미 눌렀어도 200 이다. 좋아요는 사건이 아니라 상태다. */
	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/{postId}/likes")
	public LikeResponse like(@PathVariable Long postId) {
		return postService.like(userContextService.getCurrentUserId(), postId);
	}

	/** 안 누른 상태에서 취소해도 200 이다. 같은 이유다. */
	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping("/{postId}/likes")
	public LikeResponse unlike(@PathVariable Long postId) {
		return postService.unlike(userContextService.getCurrentUserId(), postId);
	}

	/** 비로그인 허용. mine 은 전부 false 로 내려간다. */
	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{postId}/comments")
	public CommentResponse getComments(@PathVariable Long postId) {
		return postService.getComments(userContextService.getCurrentUserIdOrNull(), postId);
	}

	/** 작성 후 갱신된 목록을 그대로 돌려준다. 프론트가 다시 조회할 필요가 없다. */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/{postId}/comments")
	public CommentResponse addComment(
		@PathVariable Long postId,
		@RequestBody @Valid CommentRequest request) {

		return postService.addComment(userContextService.getCurrentUserId(), postId, request);
	}

	/** 본인 댓글만. 남의 것이면 403 이다 — 목록에 이미 공개돼 있어 숨길 것이 없다. */
	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping("/{postId}/comments/{commentId}")
	public CommentResponse deleteComment(
		@PathVariable Long postId,
		@PathVariable Long commentId) {

		return postService.deleteComment(userContextService.getCurrentUserId(), postId, commentId);
	}
}
