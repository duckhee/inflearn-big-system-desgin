package kr.co.won.view.controller;

import kr.co.won.view.service.ArticleViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/article-views")
@RequiredArgsConstructor
public class ArticleViewController {

    private final ArticleViewService viewService;


    @PostMapping(path = "/articles/{articleId}/users/{userId}")
    public Long increaseViewCountResponse(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        return viewService.increaseViewCount(articleId, userId);
    }

    @GetMapping(path = "/articles/{articleId}")
    public Long getViewCountByArticleIdResponse(@PathVariable(name = "articleId") Long articleId) {
        return viewService.findViewCountByArticleId(articleId);
    }
}
