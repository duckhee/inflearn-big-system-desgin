package kr.co.won.view.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class ArticleViewControllerTest {

    private RestClient restClient = RestClient.create("http://localhost:9003/api/article-views");


    @DisplayName(value = "01. 조회 수에 대한 생성 테스트")
    @Test
    void createArticleViewCountTests() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch countDownLatch = new CountDownLatch(10000);
        long articleId = 3l;
        long userId = 1l;
        for (int i = 0; i < 10000; ++i) {
            executorService.submit(() -> {

                restClient.post()
                        .uri("/articles/{articleId}/users/{userId}", articleId, userId)
                        .retrieve()
                        .body(Long.class);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();

        Long countView = restClient.get()
                .uri("/articles/{articleId}", articleId)
                .retrieve()
                .body(Long.class);
        System.out.println("countView = " + countView);

    }

    @DisplayName(value = "02. 조회 수에 대한 값 가져오는 테스트")
    @Test
    void findArticleViewCountTests() {

    }

}