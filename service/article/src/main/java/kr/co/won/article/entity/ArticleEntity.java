package kr.co.won.article.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
//@Entity
//@Table(name = "tbl_article")
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity {

//    @Id
    private Long articleId;

    private String title;

    private String content;

    private Long boardId; // shard Key

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;


    public static ArticleEntity createArticle(Long articleId, String title, String content, Long boardId, Long writerId) {
        ArticleEntity article = new ArticleEntity();
        article.articleId = articleId;
        article.title = title;
        article.content = content;
        article.boardId = boardId;
        article.writerId = writerId;
        article.createdAt = LocalDateTime.now();
        article.modifiedAt = LocalDateTime.now();
        return article;
    }

    public void updateArticle(String title, String content) {
        this.title = title;
        this.content = content;
        this.modifiedAt = LocalDateTime.now();
    }

}
