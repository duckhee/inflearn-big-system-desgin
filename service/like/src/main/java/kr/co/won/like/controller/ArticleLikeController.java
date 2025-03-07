package kr.co.won.like.controller;

import kr.co.won.like.service.ArticleLikeService;
import kr.co.won.like.service.response.ArticleLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/article-like")
@RequiredArgsConstructor
public class ArticleLikeController {

    private final ArticleLikeService articleLikeService;

    @GetMapping(path = "/articles/{articleId}/users/{userId}")
    public ArticleLikeResponse findArticleLikeByUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        ArticleLikeResponse findResponse = articleLikeService.findArticleLike(articleId, userId);
        return findResponse;
    }

    @PostMapping(path = "/articles/{articleId}/users/{userId}")
    public void articleLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.articleLike(articleId, userId);
    }

    @DeleteMapping(path = "/articles/{articleId}/users/{userId}")
    public void articleUnLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.articleUnLike(articleId, userId);
    }

}
