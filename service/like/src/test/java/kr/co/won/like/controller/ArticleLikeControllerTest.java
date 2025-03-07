package kr.co.won.like.controller;

import kr.co.won.like.service.response.ArticleLikeResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class ArticleLikeControllerTest {

    RestClient restClient = RestClient.create("http://localhost:9002/api/article-like");

    @DisplayName(value = "01. 좋아요 후 좋아요 취소 테스트")
    @Test
    void likeAndUnLikeTests() {
        Long articleId = 9999l;

        like(articleId, 1L);
        like(articleId, 2L);
        like(articleId, 3L);

        ArticleLikeResponse likeResponse1 = findLike(articleId, 1L);
        ArticleLikeResponse likeResponse2 = findLike(articleId, 2L);
        ArticleLikeResponse likeResponse3 = findLike(articleId, 3L);

        assertEquals(likeResponse1.getArticleId(), articleId);
        assertEquals(likeResponse2.getArticleId(), articleId);
        assertEquals(likeResponse3.getArticleId(), articleId);
        assertEquals(likeResponse1.getUserId(), 1l);
        assertEquals(likeResponse2.getUserId(), 2l);
        assertEquals(likeResponse3.getUserId(), 3l);

        unLike(articleId, 1L);
        unLike(articleId, 2L);
        unLike(articleId, 3L);
    }

    void like(Long articleId, Long userId) {
        restClient.post()
                .uri("/articles/{articleId}/users/{userId}", articleId, userId)
                .retrieve()
                .body(Void.class);
    }

    void unLike(Long articleId, Long userId) {
        restClient.delete()
                .uri("/articles/{articleId}/users/{userId}", articleId, userId)
                .retrieve()
                .body(Void.class);
    }

    ArticleLikeResponse findLike(Long articleId, Long userId) {
        return restClient.get()
                .uri("/articles/{articleId}/users/{userId}", articleId, userId)
                .retrieve()
                .body(ArticleLikeResponse.class);
    }

}