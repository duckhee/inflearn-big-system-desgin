package kr.co.won.comment.service.response;

import kr.co.won.comment.entity.CommentEntity;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class CommentResponse {

    private Long commentId;

    private Long parentCommentId;

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
        return commentResponse;
    }
}
