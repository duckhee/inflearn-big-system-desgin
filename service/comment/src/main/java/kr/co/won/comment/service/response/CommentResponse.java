package kr.co.won.comment.service.response;

import kr.co.won.comment.entity.CommentEntity;
import kr.co.won.comment.entity.InfinityCommentEntity;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.dialect.function.LpadRpadPadEmulation;

import java.time.LocalDateTime;

@Getter
@ToString
public class CommentResponse {

    private Long commentId;

    private Long parentCommentId;

    private String commentPath;

    private Long writerId;

    private String content;

    private Boolean deleted;

    private LocalDateTime createdAt;


    public static CommentResponse from(CommentEntity commentEntity) {
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.commentId = commentEntity.getCommentId();
        commentResponse.parentCommentId = commentEntity.getParentCommentId();
        commentResponse.writerId = commentEntity.getWriterId();
        commentResponse.content = commentEntity.getContent();
        commentResponse.deleted = commentEntity.getDeleted();
        commentResponse.createdAt = commentEntity.getCreatedAt();
        commentResponse.commentPath = null;
        return commentResponse;
    }

    public static CommentResponse from(InfinityCommentEntity infinityCommentEntity) {
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.commentId = infinityCommentEntity.getCommentId();
        commentResponse.writerId = infinityCommentEntity.getWriterId();
        commentResponse.content = infinityCommentEntity.getContent();
        commentResponse.deleted = infinityCommentEntity.getDeleted();
        commentResponse.createdAt = infinityCommentEntity.getCreatedAt();
        commentResponse.commentPath = infinityCommentEntity.getCommentPath().getPath();
        commentResponse.parentCommentId = null;
        return commentResponse;
    }
}
