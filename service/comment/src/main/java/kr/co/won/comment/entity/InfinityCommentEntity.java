package kr.co.won.comment.entity;

import jakarta.persistence.Embedded;
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
@Entity
@Table(name = "tbl_infinity_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class InfinityCommentEntity {

    @Id
    private Long commentId;

    private String content;

    /**
     * 자체적으로 사용을 할 Logic에 대해서 분리를 하기 위해서 Embedded로 만들어서 관심사를 분리를 해준다.
     */
    @Embedded
    private CommentPath commentPath;

    private Long articleId; // shard Key

    private Long writerId;

    private Boolean deleted;

    private LocalDateTime createdAt;

    public static InfinityCommentEntity create(Long commentId, String content, Long articleId, Long writerId, CommentPath commentPath) {
        InfinityCommentEntity comment = new InfinityCommentEntity();
        comment.commentId = commentId;
        comment.articleId = articleId;
        comment.content = content;
        comment.writerId = writerId;
        comment.commentPath = commentPath;
        comment.deleted = false;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }

    public boolean isRootComment() {
        return commentPath.isRootComment();
    }

    public void delete() {
        this.deleted = true;
    }


}
