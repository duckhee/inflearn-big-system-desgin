package kr.co.won.comment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
//@Entity
//@Table(name = "tbl_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity {

//    @Id
    private Long commentId;

    private String content;

    private Long parentCommentId;

    private Long articleId; // shard Key

    private Long writerId;

    private Boolean deleted;

    private LocalDateTime createdAt;

    public static CommentEntity create(Long commentId, String content, Long parentCommentId, Long articleId, Long writerId) {
        CommentEntity commentEntity = new CommentEntity();
        commentEntity.commentId = commentId;
        commentEntity.content = content;
        commentEntity.parentCommentId = parentCommentId == null ? commentId : parentCommentId;
        commentEntity.articleId = articleId;
        commentEntity.writerId = writerId;
        commentEntity.deleted = false;
        commentEntity.createdAt = LocalDateTime.now();
        return commentEntity;
    }

    public boolean isRoot() {
        return this.parentCommentId.longValue() == this.commentId.longValue();
    }

    public void deleteComment() {
        this.deleted = true;
    }
}
