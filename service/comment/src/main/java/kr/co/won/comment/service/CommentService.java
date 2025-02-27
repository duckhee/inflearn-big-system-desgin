package kr.co.won.comment.service;

import kr.co.won.comment.entity.CommentEntity;
import kr.co.won.comment.repository.CommentRepository;
import kr.co.won.comment.service.request.CommentCreateRequest;
import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import kr.co.won.comment.service.utils.paging.PageLimitCalculator;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.events.Comment;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final Snowflake pkGenerator = new Snowflake();

    @Transactional
    public CommentResponse createComment(CommentCreateRequest request) {
        CommentEntity findParentComment = findParentComment(request);
        CommentEntity saveComment = commentRepository.save(CommentEntity.create(pkGenerator.nextId(), request.getContent(), request.getParentCommentId() == null ? null : findParentComment.getParentCommentId(), request.getArticleId(), request.getWriterId()));
        return CommentResponse.from(saveComment);
    }

    public CommentResponse readComment(Long commentId) {
        CommentEntity findComment = commentRepository.findById(commentId)
                .orElseThrow();
        return CommentResponse.from(findComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.findById(commentId)
                .filter(Predicate.not(CommentEntity::getDeleted)) // 삭제 표시가 있는지 여부 확인
                .ifPresent(comment -> {
                    /** 하위 댓글이 있는 경우 -> 삭제에 대한 표시 */
                    if (hasSubComment(comment)) {
                        comment.deleteComment();
                    }
                    /** 하위 댓글이 없는 경우 -> DB에서 제거 */
                    else {
                        commentDeleteDB(comment);
                    }
                });
    }

    public CommentPageResponse pagingComment(Long articleId, Long pageNumber, Long pageSize) {
        Long offset = (pageNumber - 1) * pageSize;
        List<CommentResponse> pagingResponse = commentRepository.pagingComment(articleId, offset, pageSize).stream()
                .map(CommentResponse::from).toList();
        Long pageCounter = commentRepository.commentPagingNumberCount(articleId, PageLimitCalculator.calculatePageLimit(pageNumber, pageSize, 10l));

        return CommentPageResponse.of(pagingResponse, pageCounter);
    }

    public List<CommentResponse> infinityScrollComment(Long articleId, Long lastParentCommentId, Long lastCommentId, Long limit) {
        List<CommentEntity> commentResponses = lastParentCommentId == null || lastCommentId == null ? commentRepository.infinityScrollComment(articleId, limit) : commentRepository.infinityScrollComment(articleId, lastParentCommentId, lastCommentId, limit);
        return commentResponses.stream()
                .map(CommentResponse::from).toList();
    }


    private CommentEntity findParentComment(CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId == null) {
            return null;
        }

        return commentRepository.findById(parentCommentId)
                .filter(Predicate.not(CommentEntity::getDeleted))
                .filter(CommentEntity::isRoot) // 루트 댓글인지 확인
                .orElseThrow();
    }

    private boolean hasSubComment(CommentEntity comment) {

        return commentRepository.checkSubComment(comment.getArticleId(), comment.getCommentId(), 2l) == 2;

    }

    private boolean commentDeleteDB(CommentEntity commentEntity) {
        commentRepository.delete(commentEntity);
        if (!commentEntity.isRoot()) {
            commentRepository.findById(commentEntity.getParentCommentId())
                    .filter(CommentEntity::getDeleted)
                    .filter(Predicate.not(this::hasSubComment)) // 자식 댓글이 있을 경우
                    .ifPresent(this::commentDeleteDB); // 재귀적인 호출
        }
        return false;
    }

}
