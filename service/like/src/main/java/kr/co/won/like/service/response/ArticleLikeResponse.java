package kr.co.won.like.service.response;

import kr.co.won.like.entity.ArticleLikeEntity;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class ArticleLikeResponse {
    private Long articleLikeId;
    private Long articleId;
    private Long userId;
    private LocalDateTime createdAt;

    public static ArticleLikeResponse from(ArticleLikeEntity articleLikeEntity) {
        ArticleLikeResponse articleLikeResponse = new ArticleLikeResponse();
        articleLikeResponse.articleLikeId = articleLikeEntity.getArticleLikeId();
        articleLikeResponse.articleId = articleLikeEntity.getArticleId();
        articleLikeResponse.userId = articleLikeEntity.getUserId();
        articleLikeResponse.createdAt = articleLikeEntity.getCreatedAt();
        return articleLikeResponse;
    }
}
