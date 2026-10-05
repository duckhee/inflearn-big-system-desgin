package kr.co.won.article.service.response;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 페이지 번호 방식 목록 응답 [원본 코드 유지]
 *
 * - v1 API 는 이 객체를 그대로 JSON 으로 응답한다. (article-read 서비스의 ArticleClient 가 이 형식에 의존)
 * - v2(HATEOAS) API 는 이 객체를 Service 에서 받은 뒤 Controller 에서 PageImpl -> PagedModel 로 변환한다.
 *   => Service 계층은 HATEOAS 를 몰라도 된다. (웹 계층 관심사 분리)
 */
@Getter
@ToString
public class ArticlePageResponse {

    /** 현재 페이지의 게시글 목록 */
    private List<ArticleResponse> articles;

    /** 블록 제한 count (전체 게시글 수가 아님, PageLimitCalculator 참고) */
    private Long articleCount;

    protected ArticlePageResponse() {
    }

    public static ArticlePageResponse of(List<ArticleResponse> articles, Long articleCount) {
        ArticlePageResponse articlePageResponse = new ArticlePageResponse();
        articlePageResponse.articles = articles;
        articlePageResponse.articleCount = articleCount;
        return articlePageResponse;
    }
}
