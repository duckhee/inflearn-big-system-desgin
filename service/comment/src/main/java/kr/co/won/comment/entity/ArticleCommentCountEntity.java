package kr.co.won.comment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
//@Entity
//@Table(name = "tbl_article_comment_count")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleCommentCountEntity {

    @Id
    private Long articleId; // shard key

    private Long commentCount;

    public static ArticleCommentCountEntity init(Long articleId, Long commentCount) {
        ArticleCommentCountEntity entity = new ArticleCommentCountEntity();
        entity.articleId = articleId;
        entity.commentCount = commentCount;
        return entity;
    }

}
