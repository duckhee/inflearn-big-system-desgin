package kr.co.won.comment.service;

import kr.co.won.comment.entity.ArticleCommentCountEntity;
import kr.co.won.comment.entity.CommentPath;
import kr.co.won.comment.entity.InfinityCommentEntity;
import kr.co.won.comment.repository.ArticleCommentCountRepository;
import kr.co.won.comment.repository.InfinityCommentRepository;
import kr.co.won.comment.service.request.InfinityCommentCreateRequest;
import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import kr.co.won.comment.service.utils.paging.PageLimitCalculator;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.CommentCreatedEventPayload;
import kr.co.won.common.event.payload.CommentDeletedEventPayload;
import kr.co.won.common.outboxmessagerelay.event.OutboxEventPublisher;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class InfinityCommentService {

    private final InfinityCommentRepository commentRepository;
    private final ArticleCommentCountRepository commentCountRepository;

    private final Snowflake snowflake = new Snowflake();
    // kafka event 전송을 하기 위한 추가
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public CommentResponse createComment(InfinityCommentCreateRequest request) {
        InfinityCommentEntity parent = findParentComment(request);
        CommentPath parentPath = (parent == null) ? CommentPath.create("") : parent.getCommentPath();
        String findDescendantTopPath = commentRepository.findDescendantTopPath(request.getArticleId(), parentPath.getPath()).orElse(null);
        InfinityCommentEntity newComment = InfinityCommentEntity.create(snowflake.nextId(), request.getContent(), request.getArticleId(), request.getWriterId(), parentPath.getChildCommentPath(findDescendantTopPath));
        InfinityCommentEntity savedNewComment = commentRepository.save(newComment);
        /** 댓글 수에 대한 증가 */
        int commentCount = commentCountRepository.increaseCommentCount(request.getArticleId());
        if (commentCount == 0) {
            commentCountRepository.save(ArticleCommentCountEntity.init(request.getArticleId(), 1l));
        }

        // kafka event 발행
        outboxEventPublisher.publish(
                EventType.COMMENT_CREATE,
                CommentCreatedEventPayload.builder()
                        .commentId(savedNewComment.getCommentId())
                        .content(savedNewComment.getContent())
                        .articleId(savedNewComment.getArticleId())
                        .writerId(savedNewComment.getWriterId())
                        .deleted(savedNewComment.getDeleted())
                        .createdAt(savedNewComment.getCreatedAt())
                        .articleCommentCount(commentCount(savedNewComment.getArticleId()))
                        .build(),
                savedNewComment.getArticleId()
        );
        return CommentResponse.from(savedNewComment);
    }

    public CommentResponse readComment(Long commentId) {
        InfinityCommentEntity findComment = commentRepository.findById(commentId).orElseThrow();
        return CommentResponse.from(findComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.findById(commentId)
                .filter(Predicate.not(InfinityCommentEntity::getDeleted))
                .ifPresent(comment -> {
                    if (hasSubComment(comment)) {
                        comment.delete();
                    } else {
                        deleteInDatabase(comment);
                    }
                });

    }

    public Long commentCount(Long articleId) {
        return commentCountRepository.findById(articleId)
                .map(ArticleCommentCountEntity::getCommentCount)
                .orElse(0L);
    }

    private void deleteInDatabase(InfinityCommentEntity comment) {
        commentRepository.delete(comment);
        /** DB dㅔ서 삭제가 될 때에만 count 의 수를 줄여준다. */
        commentCountRepository.decreaseCommentCount(comment.getArticleId());
        /** 재귀적으로 호출을 해주기 위한 것 */
        if (!comment.isRootComment()) {
            commentRepository.findByPath(comment.getCommentPath().getPath())
                    .filter(InfinityCommentEntity::getDeleted)
                    .filter(Predicate.not(this::hasSubComment))
                    .ifPresent(this::deleteInDatabase);
        }

        // kafka event 발행
        outboxEventPublisher.publish(
                EventType.COMMENT_DELETE,
                CommentDeletedEventPayload.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .articleId(comment.getArticleId())
                        .writerId(comment.getWriterId())
                        .deleted(comment.getDeleted())
                        .createdAt(comment.getCreatedAt())
                        .articleCommentCount(commentCount(comment.getArticleId()))
                        .build(),
                comment.getArticleId()
        );
    }

    private boolean hasSubComment(InfinityCommentEntity comment) {
        return commentRepository.findDescendantTopPath(comment.getArticleId(), comment.getCommentPath().getPath())
                .isPresent();
    }


    private InfinityCommentEntity findParentComment(InfinityCommentCreateRequest request) {
        String parentPath = request.getParentPath();
        /** parent path에 대한 값이 NULL 일 경우 해당 댓글은 1Depth 에 해당이 된다. */
        if (parentPath == null) {
            return null;
        }
        return commentRepository.findByPath(parentPath)
                .filter(Predicate.not(InfinityCommentEntity::getDeleted))
                .orElseThrow();
    }

    public CommentPageResponse pagingComments(Long articleId, Long pageNumber, Long pageSize) {
        List<CommentResponse> comments = commentRepository.pagingComments(articleId, (pageNumber - 1) * pageSize, pageSize).stream()
                .map(CommentResponse::from).toList();
        Long pagingNumber = commentRepository.commentPagingNumber(articleId, PageLimitCalculator.calculatePageLimit(pageNumber, pageSize, 10l));
//        Long pagingNumber = commentCount(articleId);
        return CommentPageResponse.of(comments, pagingNumber);
    }

    public List<CommentResponse> infinityScrollComments(Long articleId, String lastPath, Long pageSize) {
        List<InfinityCommentEntity> scrollCommentsEntity = (lastPath == null) ? commentRepository.infinityCommentsScroll(articleId, pageSize) : commentRepository.infinityCommentsScroll(articleId, lastPath, pageSize);
        return scrollCommentsEntity.stream().map(CommentResponse::from).toList();
    }
}
