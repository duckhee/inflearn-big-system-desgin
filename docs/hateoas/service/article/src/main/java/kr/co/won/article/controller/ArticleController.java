package kr.co.won.article.controller;

import kr.co.won.article.service.ArticleService;
import kr.co.won.article.service.request.ArticleCreateRequest;
import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 API v1 (일반 JSON) [원본 코드 유지]
 *
 * [유지하는 이유]
 * - 내부 서비스가 이 응답 형식에 의존한다.
 *   - article-read : ArticleClient 가 목록(ArticlePageResponse) / 무한 스크롤(List) / count(Long) / 단건을 역직렬화
 *   - hot-article  : ArticleClient 가 단건 조회
 * - 이 응답을 HAL 로 바꾸면 위 서비스들이 역직렬화에 실패하므로, HATEOAS 는 v2({@link ArticleHateoasController})로 분리했다.
 * - 반환 타입이 RepresentationModel 이 아니므로 spring-hateoas 가 추가되어도 응답 형식은 바뀌지 않는다.
 */
@RestController
@RequestMapping(path = "/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ArticlePageResponse pagingArticleResponse(@RequestParam("boardId") Long boardId, @RequestParam("page") Long pageNumber, @RequestParam("size") Long pageSize) {
        return articleService.pageArticle(boardId, pageNumber, pageSize);
    }

    @GetMapping(path = "/infinity-scroll")
    public List<ArticleResponse> infinityScrollArticle(@RequestParam("boardId") Long boardId, @RequestParam("size") Long pageSize, @RequestParam(value = "lastArticleId", required = false) Long lastArticleId) {
        return articleService.infinityScrollArticle(boardId, pageSize, lastArticleId);
    }

    @GetMapping(path = "/{articleId}")
    public ArticleResponse readArticleResponse(@PathVariable Long articleId) {
        return articleService.findArticle(articleId);
    }

    @GetMapping(path = "/boards/{boardId}/article-count")
    public Long boardArticleCountResponse(@PathVariable(name = "boardId") Long boardId) {
        return articleService.articleCountNumber(boardId);
    }

    @PostMapping
    public ArticleResponse createArticleResponse(@RequestBody ArticleCreateRequest request) {
        return articleService.createArticle(request);
    }

    @PutMapping(path = "/{articleId}")
    public ArticleResponse updateArticleResponse(@PathVariable Long articleId, @RequestBody ArticleUpdateRequest request) {
        return articleService.updateArticle(articleId, request);
    }

    @DeleteMapping(path = "/{articleId}")
    public void deleteArticleResponse(@PathVariable Long articleId) {
        articleService.deleteArticle(articleId);
    }

}
