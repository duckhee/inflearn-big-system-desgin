package kr.co.won.article.service.response;

import kr.co.won.article.entity.ArticleEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class ArticlePageResponse {

    private List<ArticleResponse> articles;

    private Long articleCount;

    protected ArticlePageResponse() {
    }

    private ArticlePageResponse(List<ArticleResponse> articles, Long articleCount) {
        this.articles = articles;
        this.articleCount = articleCount;
    }

    public static ArticlePageResponse of(List<ArticleResponse> articles, Long articleCount) {
        ArticlePageResponse articlePageResponse = new ArticlePageResponse();
        articlePageResponse.articles = articles;
        articlePageResponse.articleCount = articleCount;
        return articlePageResponse;
    }
}
