package kr.co.won.articleread.controller;

import kr.co.won.articleread.service.ArticleReadService;
import kr.co.won.articleread.service.response.ArticleReadPageResponse;
import kr.co.won.articleread.service.response.ArticleReadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/article-read")
@RequiredArgsConstructor
public class ArticleReadController {

    private final ArticleReadService articleReadService;

    @GetMapping(path = "/{articleId}")
    public ArticleReadResponse articleReadResponse(@PathVariable Long articleId) {
        return articleReadService.readArticle(articleId);
    }

    @GetMapping
    public ArticleReadPageResponse articleReadPageResponse(@RequestParam(name = "boardId") Long boardId, @RequestParam(name = "page") Long page, @RequestParam(name = "size") Long pageSize) {
        return articleReadService.readArticlePageResponse(boardId, page, pageSize);
    }

    @GetMapping(path = "/infinity-scroll")
    public List<ArticleReadResponse> articleReadInfinityScroll(@RequestParam(name = "boardId") Long boardId, @RequestParam(name = "lastArticleId", required = false) Long lastArticleId, @RequestParam(name = "size") Long limit) {
        return articleReadService.readArticleInfinityScroll(boardId, lastArticleId, limit);
    }
}
