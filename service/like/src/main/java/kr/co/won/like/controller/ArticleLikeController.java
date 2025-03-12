package kr.co.won.like.controller;

import kr.co.won.like.service.ArticleLikeService;
import kr.co.won.like.service.response.ArticleLikeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.web.bind.annotation.*;

@Slf4j
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

    @GetMapping(path = "/articles/{articleId}/count")
    public Long likeCount(@PathVariable(name = "articleId") Long articleId) {
        Long likeCounter = articleLikeService.likeCount(articleId);
        log.info("article id : {}, like count : {}", articleId, likeCounter);
        return likeCounter;
    }

    /**
     * UPDATE 구문을 활용한 좋아요 수 변경
     *
     * @param articleId
     * @param userId
     */
    @PostMapping(path = "/articles/{articleId}/users/{userId}/pessimistic-lock-1")
    public void likePessimisticLockUsingArticleLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        log.info("articleId : {}, userId : {}", articleId, userId);
        articleLikeService.likePessimisticLock(articleId, userId);
    }


    @DeleteMapping(path = "/articles/{articleId}/users/{userId}/pessimistic-lock-1")
    public void unlikePessimisticLockUsingArticleUnLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.unlikePessimisticLock(articleId, userId);
    }

    /**
     * SELECT FOR UPDATE 구문을 활용한 좋아요 수 변경
     *
     * @param articleId
     * @param userId
     */
    @PostMapping(path = "/articles/{articleId}/users/{userId}/pessimistic-lock-2")
    public void likePessimisticLockUsingArticleLikeUserSelectForUpdate(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.likePessimisticLock2(articleId, userId);
    }

    @DeleteMapping(path = "/articles/{articleId}/users/{userId}/pessimistic-lock-2")
    public void unlikePessimisticLockUsingArticleUnLikeUserSelectForUpdate(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.unlikePessimisticLock2(articleId, userId);
    }

    /**
     * OPTIMISTIC LOCK(낙관적 락)을 이용한 좋아요 수 변경
     *
     * @param articleId
     * @param userId
     */
    @PostMapping(path = "/articles/{articleId}/users/{userId}/optimistic-lock")
    public void likeOptimisticLockUsingArticleLikeUserSelectForUpdate(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        try {
            articleLikeService.likeOptimisticLock(articleId, userId);
        } catch (Exception exception) {
            log.error("[{}][{}]exception : {}", exception.getClass().getName(), exception.getCause(), exception.getMessage());
        }
    }

    @DeleteMapping(path = "/articles/{articleId}/users/{userId}/optimistic-lock")
    public void unlikeOptimisticLockUsingArticleUnLikeUserSelectForUpdate(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.unlikeOptimisticLock(articleId, userId);
    }


    /**
     * 좋아요 수에 대한 반영이 없는 호출
     *
     * @param articleId
     * @param userId
     */
    @PostMapping(path = "/articles/{articleId}/users/{userId}")
    public void articleLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.articleLike(articleId, userId);
    }

    @DeleteMapping(path = "/articles/{articleId}/users/{userId}")
    public void articleUnLikeUser(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "userId") Long userId) {
        articleLikeService.articleUnLike(articleId, userId);
    }

}
