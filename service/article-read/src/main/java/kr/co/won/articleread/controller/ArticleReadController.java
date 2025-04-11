package kr.co.won.articleread.controller;

import kr.co.won.articleread.service.ArticleReadService;
import kr.co.won.articleread.service.response.ArticleReadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
