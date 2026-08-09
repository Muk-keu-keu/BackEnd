package mukkeu.mukkeu.post.app;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mukkeu.mukkeu.global.exception.BusinessException;
import mukkeu.mukkeu.global.exception.domain.ErrorCode;
import mukkeu.mukkeu.order.app.OrderService;
import mukkeu.mukkeu.order.domain.repository.OrderRepository;
import mukkeu.mukkeu.post.domain.ImageStorage;
import mukkeu.mukkeu.post.domain.Post;
import mukkeu.mukkeu.post.domain.PostComment;
import mukkeu.mukkeu.post.domain.PostImage;
import mukkeu.mukkeu.post.domain.PostLike;
import mukkeu.mukkeu.post.domain.repository.PostCommentRepository;
import mukkeu.mukkeu.post.domain.repository.PostImageRepository;
import mukkeu.mukkeu.post.domain.repository.PostLikeRepository;
import mukkeu.mukkeu.post.domain.repository.PostRepository;
import mukkeu.mukkeu.post.dto.CommentRequest;
import mukkeu.mukkeu.post.dto.CommentResponse;
import mukkeu.mukkeu.post.dto.LikeResponse;
import mukkeu.mukkeu.post.dto.PostCreateRequest;
import mukkeu.mukkeu.post.dto.PostCreateResponse;
import mukkeu.mukkeu.post.dto.PostDetailResponse;
import mukkeu.mukkeu.post.dto.PostListResponse;
import mukkeu.mukkeu.post.dto.PostUpdateRequest;
import mukkeu.mukkeu.user.domain.User;
import mukkeu.mukkeu.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 요기족보 — 내가 시킨 조합을 자랑하는 게시판.
 *
 * 글은 결제 단위다. 영상 속 조합이 여러 가게에 걸치므로 가게가 아니라 checkoutId 를 문다.
 * 그래서 상세에 결제 내역이 통째로 들어가고, 남이 그걸 그대로 장바구니에 담을 수 있다.
 * "따라담기" 에 별도 API 를 두지 않는 이유다 — 장바구니는 프론트 상태이고
 * 결제는 이미 POST v1/orders 가 있다.
 *
 * 조회는 비로그인도 허용한다. 그때 liked 는 전부 false 다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 50;
	private static final int MAX_IMAGES = 5;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final PostLikeRepository postLikeRepository;
	private final PostCommentRepository postCommentRepository;
	private final OrderRepository orderRepository;
	private final OrderService orderService;
	private final UserRepository userRepository;
	private final ImageStorage imageStorage;

	// ────────────────────────────────────────────────────────
	//  작성
	// ────────────────────────────────────────────────────────

	/**
	 * 이미지 업로드가 실패하면 글도 남기지 않는다. 사진이 빈 자랑글은 화면이 깨진다.
	 * 업로드를 먼저 하고 저장을 나중에 하므로, 실패 시 버킷에 고아 파일이 남을 수는 있다.
	 * 그건 나중에 정리하면 되지만 DB 에 깨진 글이 남는 것은 사용자가 바로 본다.
	 */
	@Transactional
	public PostCreateResponse create(Long userId, PostCreateRequest request, List<MultipartFile> images) {

		// 내 결제여야 한다. 남의 결제 번호를 넣어도 없는 것과 똑같이 404 다.
		if (!orderRepository.existsByCheckoutIdAndUserId(request.checkoutId(), userId)) {
			throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
		}
		// 한 결제에 글 하나. DB 의 uq_post_checkout 이 최종 방어선이고 여기서 미리 거른다.
		if (postRepository.existsByCheckoutId(request.checkoutId())) {
			throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS);
		}
		if (images != null && images.size() > MAX_IMAGES) {
			throw new BusinessException(ErrorCode.TOO_MANY_IMAGES);
		}

		List<String> urls = imageStorage.upload(images);

		Post post = postRepository.save(Post.builder()
			.userId(userId)
			.checkoutId(request.checkoutId())
			.title(request.title())
			.body(request.body())
			.build());

		if (!urls.isEmpty()) {
			List<PostImage> rows = new ArrayList<>();
			for (int i = 0; i < urls.size(); i++) {
				rows.add(PostImage.builder()
					.postId(post.getId())
					.imageUrl(urls.get(i))
					.sortOrder(i)          // 올린 순서를 화면 순서로 그대로 쓴다
					.build());
			}
			postImageRepository.saveAll(rows);
		}

		return new PostCreateResponse(post.getId());
	}

	// ────────────────────────────────────────────────────────
	//  수정 · 삭제
	// ────────────────────────────────────────────────────────

	/**
	 * 본인 글만. 남의 글이면 403 이다 — 목록에 이미 공개돼 있어 숨길 것이 없다.
	 *
	 * 이미지는 최종 상태를 받아 통째로 다시 깐다. keepImageUrls 순서가 곧 새 sort_order 고,
	 * 새 파일은 그 뒤에 붙는다. 행을 지웠다 다시 넣는 이유는, 남은 것만 골라 번호를
	 * 다시 매기려면 어차피 전부 UPDATE 해야 해서 이득이 없기 때문이다.
	 *
	 * 목록에서 빠진 이미지는 DB 에서만 지우고 버킷 파일은 남긴다. 고아 파일이 쌓이지만
	 * 지우다 실패하면 화면에 없는 사진 때문에 수정이 통째로 롤백된다. 저장 공간보다
	 * 사용자가 글을 고칠 수 있는 쪽이 중요하다.
	 */
	@Transactional
	public PostDetailResponse update(Long userId, Long postId,
		PostUpdateRequest request, List<MultipartFile> images) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		if (!post.isOwnedBy(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
		}

		// 폼 필드로 오므로 빈 값 한 줄이 [""] 로 들어올 수 있다. 그대로 두면
		// "이 글 이미지가 맞나" 검사에 걸려 400 이 난다. 빈 값은 없는 것으로 본다.
		List<String> keep = request.keepImageUrls() == null ? List.of()
			: request.keepImageUrls().stream().filter(u -> u != null && !u.isBlank()).toList();
		int newCount = images == null ? 0 : images.size();
		if (keep.size() + newCount > MAX_IMAGES) {
			throw new BusinessException(ErrorCode.TOO_MANY_IMAGES);
		}

		// 남의 글 이미지 URL 을 keepImageUrls 에 넣어 가져오는 것을 막는다.
		Set<String> owned = postImageRepository.findAllByPostId(postId).stream()
			.map(PostImage::getImageUrl).collect(Collectors.toSet());
		if (!owned.containsAll(keep)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		post.update(request.title(), request.body());

		List<String> uploaded = imageStorage.upload(images);

		postImageRepository.deleteAllByPostId(postId);
		List<String> finalUrls = new ArrayList<>(keep);
		finalUrls.addAll(uploaded);

		if (!finalUrls.isEmpty()) {
			List<PostImage> rows = new ArrayList<>();
			for (int i = 0; i < finalUrls.size(); i++) {
				rows.add(PostImage.builder()
					.postId(postId).imageUrl(finalUrls.get(i)).sortOrder(i).build());
			}
			postImageRepository.saveAll(rows);
		}

		return getDetail(userId, postId);
	}

	/**
	 * 본인 글만. 자식 행을 먼저 지운다 — FK 가 걸려 있어 순서를 지키지 않으면
	 * ORA-02292 로 실패한다. 좋아요·댓글도 같이 사라지는 것이 맞다.
	 * 글이 없어졌는데 그 글의 댓글만 남을 이유가 없다.
	 */
	@Transactional
	public void delete(Long userId, Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		if (!post.isOwnedBy(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
		}

		postCommentRepository.deleteAllByPostId(postId);
		postLikeRepository.deleteAllByPostId(postId);
		postImageRepository.deleteAllByPostId(postId);
		postRepository.delete(post);
	}

	// ────────────────────────────────────────────────────────
	//  홈 목록
	// ────────────────────────────────────────────────────────

	/**
	 * @param viewerId 로그인했으면 userId, 아니면 null. liked 를 채울지 결정한다.
	 * @param sort     LATEST | POPULAR
	 */
	@Transactional(readOnly = true)
	public PostListResponse getList(Long viewerId, String sort, String cursor, Integer size) {

		int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		boolean popular = "POPULAR".equalsIgnoreCase(sort);

		List<Post> posts = popular
			? postRepository.findPopular(likeCursorOf(cursor), idCursorOf(cursor, true), limit)
			: postRepository.findLatest(idCursorOf(cursor, false), limit);

		if (posts.isEmpty()) {
			return new PostListResponse(List.of(), null);
		}

		List<Long> postIds = posts.stream().map(Post::getId).toList();

		// 썸네일·닉네임·좋아요 여부를 각각 한 번씩만 읽는다. 글마다 읽으면 N+1 이다.
		Map<Long, String> thumbnailByPost = firstImageByPost(postIds);
		Map<Long, String> nickNameByUser = nickNameByUser(posts.stream().map(Post::getUserId).toList());
		Set<Long> likedPostIds = likedPostIds(viewerId, postIds);

		List<PostListResponse.Card> cards = posts.stream()
			.map(p -> new PostListResponse.Card(
				p.getId(), p.getTitle(),
				thumbnailByPost.get(p.getId()),
				nickNameByUser.get(p.getUserId()),
				toOffset(p.getCreatedAt()),
				p.getLikeCount(), p.getCommentCount(),
				likedPostIds.contains(p.getId())))
			.toList();

		// 받은 수가 요청보다 적으면 마지막 페이지다.
		String nextCursor = null;
		if (posts.size() >= limit) {
			Post last = posts.get(posts.size() - 1);
			nextCursor = popular
				? last.getLikeCount() + ":" + last.getId()
				: String.valueOf(last.getId());
		}

		return new PostListResponse(cards, nextCursor);
	}

	// ────────────────────────────────────────────────────────
	//  단건
	// ────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public PostDetailResponse getDetail(Long viewerId, Long postId) {

		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

		List<String> imageUrls = postImageRepository.findAllByPostId(postId).stream()
			.sorted(java.util.Comparator.comparingInt(PostImage::getSortOrder))
			.map(PostImage::getImageUrl)
			.toList();

		// 결제 내역은 글쓴이 것을 읽는다. 보는 사람 기준으로 읽으면 남의 글에서 404 가 난다.
		var order = orderService.getDetail(post.getUserId(), post.getCheckoutId());

		String nickName = userRepository.findById(post.getUserId())
			.map(User::getNickName).orElse(null);

		boolean liked = viewerId != null && postLikeRepository.exists(postId, viewerId);

		return new PostDetailResponse(
			post.getId(), post.getTitle(), post.getBody(), imageUrls,
			nickName, post.getUserId(),
			toOffset(post.getCreatedAt()),
			post.getLikeCount(), post.getCommentCount(),
			liked,
			viewerId != null && viewerId.equals(post.getUserId()),
			order);
	}

	// ────────────────────────────────────────────────────────
	//  좋아요
	// ────────────────────────────────────────────────────────

	/**
	 * 이미 눌렀으면 아무것도 하지 않고 현재 상태만 돌려준다. 409 가 아니다.
	 * 좋아요는 사건이 아니라 상태라서, 원하는 상태로 맞춰 달라는 요청이 이미 그 상태면
	 * 할 일이 없을 뿐 실패가 아니다.
	 */
	@Transactional
	public LikeResponse like(Long userId, Long postId) {
		requirePost(postId);

		if (!postLikeRepository.exists(postId, userId)) {
			postLikeRepository.save(new PostLike(postId, userId));
			// 실제로 행이 생겼을 때만 센다. 여기서 무조건 올리면 두 번 누른 사람이 2 를 만든다.
			postRepository.increaseLikeCount(postId);
		}
		return new LikeResponse(postRepository.readLikeCount(postId), true);
	}

	@Transactional
	public LikeResponse unlike(Long userId, Long postId) {
		requirePost(postId);

		if (postLikeRepository.exists(postId, userId)) {
			postLikeRepository.delete(postId, userId);
			postRepository.decreaseLikeCount(postId);
		}
		return new LikeResponse(postRepository.readLikeCount(postId), false);
	}

	// ────────────────────────────────────────────────────────
	//  댓글
	// ────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public CommentResponse getComments(Long viewerId, Long postId) {
		requirePost(postId);

		List<PostComment> comments = postCommentRepository.findAllByPostId(postId);
		if (comments.isEmpty()) {
			return new CommentResponse(List.of());
		}

		Map<Long, String> nickNameByUser =
			nickNameByUser(comments.stream().map(PostComment::getUserId).toList());

		return new CommentResponse(comments.stream()
			.map(c -> new CommentResponse.Comment(
				c.getId(), c.getUserId(), nickNameByUser.get(c.getUserId()),
				c.getBody(), toOffset(c.getCreatedAt()),
				viewerId != null && viewerId.equals(c.getUserId())))
			.toList());
	}

	@Transactional
	public CommentResponse addComment(Long userId, Long postId, CommentRequest request) {
		requirePost(postId);

		postCommentRepository.save(PostComment.builder()
			.postId(postId).userId(userId).body(request.body()).build());
		postRepository.increaseCommentCount(postId);

		return getComments(userId, postId);
	}

	/**
	 * 본인 댓글만 지울 수 있다. 여기서는 403 이 맞다.
	 * 주문은 남의 것이 존재한다는 사실 자체를 숨겨야 해서 404 였지만, 댓글은 이미
	 * 목록에 공개돼 있어 숨길 것이 없다.
	 */
	@Transactional
	public CommentResponse deleteComment(Long userId, Long postId, Long commentId) {
		requirePost(postId);

		PostComment comment = postCommentRepository.findById(commentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

		// 다른 글의 댓글 번호를 넣는 경우도 막는다.
		if (!comment.getPostId().equals(postId)) {
			throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
		}
		if (!comment.isOwnedBy(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
		}

		postCommentRepository.delete(comment);
		postRepository.decreaseCommentCount(postId);

		return getComments(userId, postId);
	}

	// ────────────────────────────────────────────────────────
	//  잡다
	// ────────────────────────────────────────────────────────

	private void requirePost(Long postId) {
		if (postRepository.findById(postId).isEmpty()) {
			throw new BusinessException(ErrorCode.POST_NOT_FOUND);
		}
	}

	private Map<Long, String> firstImageByPost(List<Long> postIds) {
		return postImageRepository.findAllByPostIdIn(postIds).stream()
			.sorted(java.util.Comparator.comparingInt(PostImage::getSortOrder))
			.collect(Collectors.toMap(
				PostImage::getPostId, PostImage::getImageUrl, (first, later) -> first));
	}

	private Map<Long, String> nickNameByUser(List<Long> userIds) {
		List<Long> distinct = userIds.stream().distinct().toList();
		if (distinct.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllByIdIn(distinct).stream()
			.filter(u -> u.getNickName() != null)
			.collect(Collectors.toMap(User::getId, User::getNickName, (a, b) -> a));
	}

	private Set<Long> likedPostIds(Long viewerId, List<Long> postIds) {
		if (viewerId == null) {
			return Set.of();
		}
		return new HashSet<>(postLikeRepository.findLikedPostIds(viewerId, postIds));
	}

	/** 커서는 불투명한 문자열이다. LATEST 는 "9012", POPULAR 는 "37:9012" 모양이다. */
	private Long idCursorOf(String cursor, boolean popular) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String id = popular ? cursor.substring(cursor.indexOf(':') + 1) : cursor;
			return Long.valueOf(id.trim());
		} catch (RuntimeException e) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	private Integer likeCursorOf(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(cursor.substring(0, cursor.indexOf(':')).trim());
		} catch (RuntimeException e) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	/** LocalDateTime 은 지역 정보가 없어 그대로 내보내면 프론트가 시각을 확정할 수 없다. */
	private OffsetDateTime toOffset(LocalDateTime time) {
		return time == null ? null : time.atZone(KST).toOffsetDateTime();
	}
}
