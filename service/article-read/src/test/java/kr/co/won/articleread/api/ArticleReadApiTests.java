package kr.co.won.articleread.api;


import kr.co.won.articleread.service.response.ArticleReadPageResponse;
import kr.co.won.articleread.service.response.ArticleReadResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class ArticleReadApiTests {

    RestClient articleReadRestClient = RestClient.create("http://localhost:9005");
    RestClient articleRestClient = RestClient.create("http://localhost:9000");

    @Disabled
    @DisplayName(value = "01. article read service에 대한 읽기 기능에 대한 테스트")
    @Test
    void readArticleTests() {
        ArticleReadResponse response = articleReadRestClient.get()
                .uri("/api/article-read/{articleId}", 168960288432340992l) // 임의 생성한 값
                .retrieve()
                .body(ArticleReadResponse.class);

        System.out.println("response = " + response);
    }

    @DisplayName(value = "02. article read all Test 원본과 read service 에 대한 데이터 확인 테스트")
    @Test
    void readArticleAllTests() {
        ArticleReadPageResponse readPageResponse = articleReadRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/article-read")
                        .queryParam("boardId", 1l)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(ArticleReadPageResponse.class);
        System.out.println("readPageResponse = " + readPageResponse);
        for (ArticleReadResponse article : readPageResponse.getArticles()) {
            System.out.println("article = " + article.getArticleId());
        }

        ArticleReadPageResponse articleOriginalPageResponse = articleRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/articles")
                        .queryParam("boardId", 1l)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(ArticleReadPageResponse.class);
        System.out.println("articleOriginalPageResponse = " + articleOriginalPageResponse);
        for (ArticleReadResponse article : articleOriginalPageResponse.getArticles()) {
            System.out.println("article = " + article.getArticleId());
        }
    }


    @DisplayName(value = "02. article read all Test 원본과 read service 에 대한 데이터 확인 테스트 -> 오래된 데이터는 원본 서비스에 요청을 해서 가져온다.")
    @Test
    void oldReadArticleAllTests() {
        ArticleReadPageResponse readPageResponse = articleReadRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/article-read")
                        .queryParam("boardId", 1l)
                        .queryParam("page", 30000)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(ArticleReadPageResponse.class);
        System.out.println("readPageResponse = " + readPageResponse);
        for (ArticleReadResponse article : readPageResponse.getArticles()) {
            System.out.println("article = " + article.getArticleId());
        }

        ArticleReadPageResponse articleOriginalPageResponse = articleRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/articles")
                        .queryParam("boardId", 1l)
                        .queryParam("page", 30000)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(ArticleReadPageResponse.class);
        System.out.println("articleOriginalPageResponse = " + articleOriginalPageResponse);
        for (ArticleReadResponse article : articleOriginalPageResponse.getArticles()) {
            System.out.println("article = " + article.getArticleId());
        }
    }

    @DisplayName(value = "03. infinity scroll 방식의 동작 확인 테스트")
    @Test
    void infinityScrollTests() {
        List<ArticleReadResponse> readInfinityResponse = articleReadRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/article-read/infinity-scroll")
                        .queryParam("boardId", 1l)
                        .queryParam("size", 10)
                        .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<List<ArticleReadResponse>>() {
                });
        for (ArticleReadResponse articleReadResponse : readInfinityResponse) {
            System.out.println("article = " + articleReadResponse.getArticleId());
        }

        List<ArticleReadResponse> infinityResponse = articleRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/articles/infinity-scroll")
                        .queryParam("boardId", 1)
                        .queryParam("size", 10)
                        .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<List<ArticleReadResponse>>() {
                });

        for (ArticleReadResponse articleReadResponse : infinityResponse) {
            System.out.println("article = " + articleReadResponse.getArticleId());
        }


    }

}
