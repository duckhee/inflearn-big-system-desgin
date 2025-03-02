package kr.co.won.comment.service;

import kr.co.won.comment.entity.CommentPath;
import kr.co.won.comment.entity.InfinityCommentEntity;
import kr.co.won.comment.repository.InfinityCommentRepository;
import kr.co.won.comment.service.request.InfinityCommentCreateRequest;
import kr.co.won.comment.service.response.CommentResponse;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class InfinityCommentService {

    private final InfinityCommentRepository commentRepository;

    private final Snowflake snowflake = new Snowflake();


    public CommentResponse createComment(InfinityCommentCreateRequest request) {
        InfinityCommentEntity parent = findParentComment(request);
        CommentPath parentPath = (parent == null) ? CommentPath.create("") : parent.getCommentPath();
        String findDescendantTopPath = commentRepository.findDescendantTopPath(request.getArticleId(), parentPath.getPath()).orElse(null);
        InfinityCommentEntity newComment = InfinityCommentEntity.create(snowflake.nextId(), request.getContent(), request.getArticleId(), request.getWriterId(), parentPath.getChildCommentPath(findDescendantTopPath));
        InfinityCommentEntity savedNewComment = commentRepository.save(newComment);
        return CommentResponse.from(savedNewComment);
    }

    public CommentResponse readComment(Long commentId) {
        InfinityCommentEntity findComment = commentRepository.findById(commentId).orElseThrow();
        return CommentResponse.from(findComment);
    }

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

    private void deleteInDatabase(InfinityCommentEntity comment) {
        commentRepository.delete(comment);
        /** 재귀적으로 호출을 해주기 위한 것 */
        if (!comment.isRootComment()) {
            commentRepository.findByPath(comment.getCommentPath().getPath())
                    .filter(InfinityCommentEntity::getDeleted)
                    .filter(Predicate.not(this::hasSubComment))
                    .ifPresent(this::deleteInDatabase);
        }
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

}
