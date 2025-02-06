package kr.co.won.article.service.response;

import kr.co.won.article.entity.ArticleEntity;
import kr.co.won.article.repository.ArticleRepository;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class ArticleResponse {

    private Long articleId;

    private String title;

    private String content;

    private Long boardId; // shard Key

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    public static ArticleResponse fromEntity(ArticleEntity articleEntity) {
        ArticleResponse articleResponse = new ArticleResponse();
        articleResponse.articleId = articleEntity.getArticleId();
        articleResponse.title = articleEntity.getTitle();
        articleResponse.content = articleEntity.getContent();
        articleResponse.boardId = articleEntity.getBoardId();
        articleResponse.writerId = articleEntity.getWriterId();
        articleResponse.createdAt = articleEntity.getCreatedAt();
        articleResponse.modifiedAt = articleEntity.getModifiedAt();
        return articleResponse;
    }
}
