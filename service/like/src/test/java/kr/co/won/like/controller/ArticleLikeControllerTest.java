package kr.co.won.like.controller;

import kr.co.won.like.service.response.ArticleLikeResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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


    @DisplayName(value = "02. 좋아요 수에 대한 Lock을 이용한 구현을 확인")
    @Test
    void likePerformanceTests() throws InterruptedException {
        /** Thread 를 생성해서 동시성에 대한 확인 */
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        likePerformanceFunction(executorService, 1111l, "pessimistic-lock-1");
        likePerformanceFunction(executorService, 2222l, "pessimistic-lock-2");
        likePerformanceFunction(executorService, 3333l, "optimistic-lock");
//        likeWithLock(3333l, 332131254133l, "optimistic-lock");
    }

    void likePerformanceFunction(ExecutorService executorService, Long articleId, String lockType) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3000);

        likeWithLock(articleId, 1l, lockType);

        System.out.println(String.format("[%s] start", lockType));
        long start = System.nanoTime();
        /** multi thread 로 호출 */
        for (int i = 0; i < 3000; i++) {
            long userId = i + 2;
            executorService.submit(() -> {
                likeWithLock(articleId, userId, lockType);
                countDownLatch.countDown();
            });
        }

        countDownLatch.await();

        long end = System.nanoTime();

        System.out.println("lockType=" + lockType + ", time : " + (end - start) / 1000000 + "ms");

        System.out.println(String.format("[%s end]", lockType));

        /** 좋아요 수 확인을 위한 값 가져오기 */
//        long likeCount = getLikeCount(articleId);
        Long likeCount = restClient.get()
                .uri("/articles/{articleId}/count", articleId)
                .retrieve()
                .body(Long.class);
        System.out.println("likeCount = " + likeCount);

    }

    void likeWithLock(Long articleId, Long userId, String lockType) {
        String callURI = String.format("/articles/%d/users/%d/%s", articleId, userId, lockType);
//        System.out.println("callURI = " + callURI);
        restClient.post()
                .uri(callURI)
                .retrieve()
                .body(Void.class);

    }

    void unlikeWithLock(Long articleId, Long userId, String lockType) {
        String callURI = String.format("/articles/{articleId}/users/{userId}/%s", lockType);
//        System.out.println("callURI = " + callURI);
        restClient.delete()
                .uri(callURI, articleId, userId)
                .retrieve()
                .body(Void.class);
    }

    long getLikeCount(Long articleId) {
        Long countNumber = restClient.get()
                .uri("/articles/{articleId}/count", articleId)
                .retrieve()
                .body(Long.class);
        return countNumber;
    }
}
