package kr.co.won.article.controller;

import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArticleControllerTest {

    RestClient restClient = RestClient.create("http://127.0.0.1:9000");


    @DisplayName(value = "01. create article Tests")
    @Test
    void createTests() {

        ArticleCreateRequest request = new ArticleCreateRequest("hi", "my content", 1L, 1L);
        ArticleResponse response = createArticleResponse(request);
        assertEquals(request.boardId, response.getBoardId());
    }


    ArticleResponse createArticleResponse(ArticleCreateRequest request) {
        return restClient.post()
                .uri("/api/articles")
                .body(request)
                .retrieve()
                .body(ArticleResponse.class);
    }


    @DisplayName(value = "02. read article Tests")
    @Test
    void readTests() {
        ArticleCreateRequest request = new ArticleCreateRequest("hi", "my content", 1L, 1L);
        ArticleResponse response = createArticleResponse(request);

        ArticleResponse articleResponse = readArticleResponse(response.getArticleId());
    }

    ArticleResponse readArticleResponse(Long articleId) {
        return restClient.get()
                .uri("/api/articles/{articleId}", articleId)
                .retrieve()
                .body(ArticleResponse.class);
    }

    @DisplayName(value = "03. update article Tests")
    @Test
    void updateTests() {
        ArticleCreateRequest request = new ArticleCreateRequest("hi", "my content", 1L, 1L);
        ArticleResponse response = createArticleResponse(request);
        ArticleUpdateRequest updateRequest = new ArticleUpdateRequest("update", "testing");
        ArticleResponse updateResponse = updateArticleResponse(response.getArticleId(), updateRequest);

        assertEquals(updateResponse.getTitle(), "update");

    }

    ArticleResponse updateArticleResponse(Long articleId, ArticleUpdateRequest request) {
        return restClient.put()
                .uri("/api/articles/{articleId}", articleId)
                .body(request)
                .retrieve()
                .body(ArticleResponse.class);
    }

    @DisplayName(value = "04. delete article Tests")
    @Test
    void deleteTests() {
        ArticleCreateRequest request = new ArticleCreateRequest("hi", "my content", 1L, 1L);
        ArticleResponse response = createArticleResponse(request);
        deleteArticleResponse(response.getArticleId());

        assertThrows(Exception.class, () -> readArticleResponse(response.getArticleId()));
    }

    void deleteArticleResponse(Long articleId) {
        restClient.delete()
                .uri("/api/articles/{articleId}", articleId)
                .retrieve()
                .body(Void.class);
    }

    @DisplayName(value = "05. paging article Tests")
    @Test
    void pagingTests() {
        Long boardId = 1l;
        Long pageNumber = 2l;
        Long pageSize = 10l;
        ArticlePageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/articles")
                        .queryParam("boardId", boardId)
                        .queryParam("page", pageNumber)
                        .queryParam("size", pageSize)
                        .build())
                .retrieve()
                .body(ArticlePageResponse.class);


    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class ArticleCreateRequest {

        private String title;

        private String content;

        private Long writerId;

        private Long boardId;

    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class ArticleUpdateRequest {

        private String title;

        private String content;

    }

}