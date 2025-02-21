package kr.co.won.article.controller;

import kr.co.won.article.service.ArticleService;
import kr.co.won.article.service.request.ArticleCreateRequest;
import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<ArticleResponse> infinityScrollArticle(@RequestParam("boardId") Long boardId, @RequestParam("size") Long pageSize, @RequestParam("lastArticleId") Long lastArticleId) {
        return articleService.infinityScrollArticle(boardId, pageSize, lastArticleId);
    }

    @GetMapping(path = "/{articleId}")
    public ArticleResponse readArticleResponse(@PathVariable Long articleId) {
        return articleService.findArticle(articleId);
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
