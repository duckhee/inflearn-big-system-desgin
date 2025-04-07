package kr.co.won.hotarticle.service.response;

import kr.co.won.hotarticle.client.response.ArticleResponse;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class HotArticleResponse {

    private Long articleId;

    private String title;

    private LocalDateTime createdAt;

    public static HotArticleResponse from(ArticleResponse articleResponse) {
        HotArticleResponse response = new HotArticleResponse();
        response.articleId = articleResponse.getArticleId();
        response.title = articleResponse.getTitle();
        response.createdAt = articleResponse.getCreatedAt();
        return response;
    }
}
