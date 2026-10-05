package kr.co.won.article.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v2(HATEOAS) API 통합 테스트 [HATEOAS 신규]
 *
 * - 실행 중인 서버(127.0.0.1:9000) + 실제 DB 가 필요하다. (원본 ArticleControllerTest 와 같은 방식)
 * - HAL 응답은 JsonNode 로 받아 JSON Pointer(/_links/next/href) 로 검증한다.
 *   => MappingJackson2HttpMessageConverter 가 application/*+json 을 지원하므로 application/hal+json 도 읽을 수 있다.
 * - 02 / 04 / 08 / 10 은 URL 을 직접 만들지 않고 "응답의 링크를 따라가는" HATEOAS 사용 방식 자체를 검증한다.
 * - DB 없이 링크 생성 규칙만 확인하려면 ArticleHateoasControllerWebMvcTest 를 실행한다.
 */
class ArticleHateoasControllerTest {

    RestClient restClient = RestClient.create("http://127.0.0.1:9000");

    @DisplayName(value = "01. create article -> 201 Created + Location + self link")
    @Test
    void createTests() {
        ResponseEntity<JsonNode> response = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(response.getHeaders().getLocation().toString(), response.getBody().at("/_links/self/href").asText());
        assertTrue(MediaType.parseMediaType("application/hal+json").isCompatibleWith(response.getHeaders().getContentType()));
    }

    @DisplayName(value = "02. read article -> self / articles / board-article-count links")
    @Test
    void readTests() {
        JsonNode created = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L)).getBody();
        String selfHref = created.at("/_links/self/href").asText();

        // 링크를 그대로 따라간다 (URL 하드코딩 X)
        JsonNode article = restClient.get().uri(selfHref).retrieve().body(JsonNode.class);

        assertEquals(created.get("articleId").asLong(), article.get("articleId").asLong());
        assertTrue(article.at("/_links/articles/href").asText().contains("boardId=1&page=1&size=10"));
        assertTrue(article.at("/_links/board-article-count/href").asText().endsWith("/api/v2/articles/boards/1/article-count"));
    }

    @DisplayName(value = "03. paging -> 1-based page, first/prev/self/next/last links")
    @Test
    void pagingTests() {
        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v2/articles")
                        .queryParam("boardId", 1L)
                        .queryParam("page", 2L)
                        .queryParam("size", 10L)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        assertEquals(10, body.at("/_embedded/articles").size());
        assertEquals(2, body.at("/page/number").asInt());          // one-indexed 메타데이터
        assertEquals(10, body.at("/page/size").asInt());
        assertTrue(body.at("/_links/self/href").asText().contains("page=2"));
        assertTrue(body.at("/_links/prev/href").asText().contains("page=1"));
        assertTrue(body.at("/_links/next/href").asText().contains("page=3"));
        assertTrue(body.at("/_links/first/href").asText().contains("page=1"));
        assertTrue(body.at("/_links/self/href").asText().contains("boardId=1")); // 다른 파라미터 유지
    }

    @DisplayName(value = "04. paging -> next 링크를 따라가면 다음 페이지")
    @Test
    void pagingFollowNextLinkTests() {
        JsonNode first = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=10").retrieve().body(JsonNode.class);
        JsonNode second = restClient.get().uri(first.at("/_links/next/href").asText()).retrieve().body(JsonNode.class);

        assertEquals(2, second.at("/page/number").asInt());
        long firstLastId = first.at("/_embedded/articles").get(9).get("articleId").asLong();
        long secondFirstId = second.at("/_embedded/articles").get(0).get("articleId").asLong();
        assertTrue(firstLastId > secondFirstId); // article_id DESC 정렬 유지
    }

    @DisplayName(value = "05. paging -> 블록 제한 count (page 1~10 은 totalElements <= size * 10 + 1)")
    @Test
    void pagingBlockLimitTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=10").retrieve().body(JsonNode.class);
        assertTrue(body.at("/page/totalElements").asLong() <= 101L);
    }

    @DisplayName(value = "06. paging -> max-page-size 제한")
    @Test
    void pagingMaxSizeTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=1000").retrieve().body(JsonNode.class);
        assertEquals(50, body.at("/page/size").asInt());
    }

    @DisplayName(value = "07. paging -> 빈 게시판은 _embedded.articles 빈 배열")
    @Test
    void pagingEmptyTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=999999&page=1&size=10").retrieve().body(JsonNode.class);
        assertTrue(body.at("/_embedded/articles").isArray());
        assertEquals(0, body.at("/_embedded/articles").size());
        assertTrue(body.at("/_links/next").isMissingNode());
    }

    @DisplayName(value = "08. infinity scroll -> next 링크의 lastArticleId 는 마지막 항목 ID")
    @Test
    void infinityScrollTests() {
        JsonNode first = restClient.get().uri("/api/v2/articles/infinity-scroll?boardId=1&size=10").retrieve().body(JsonNode.class);

        JsonNode articles = first.at("/_embedded/articles");
        long lastArticleId = articles.get(articles.size() - 1).get("articleId").asLong();
        String nextHref = first.at("/_links/next/href").asText();
        assertTrue(nextHref.contains("lastArticleId=" + lastArticleId));

        JsonNode second = restClient.get().uri(nextHref).retrieve().body(JsonNode.class);
        assertTrue(second.at("/_embedded/articles").get(0).get("articleId").asLong() < lastArticleId);
    }

    @DisplayName(value = "09. board article count -> DTO + links")
    @Test
    void boardCountTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles/boards/{boardId}/article-count", 1L).retrieve().body(JsonNode.class);
        assertEquals(1L, body.get("boardId").asLong());
        assertTrue(body.has("articleCount"));
        assertTrue(body.at("/_links/articles/href").asText().contains("boardId=1"));
    }

    @DisplayName(value = "10. update / delete")
    @Test
    void updateAndDeleteTests() {
        JsonNode created = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L)).getBody();
        String selfHref = created.at("/_links/self/href").asText();

        JsonNode updated = restClient.put().uri(selfHref)
                .body(new ArticleUpdateRequest("update", "testing"))
                .retrieve()
                .body(JsonNode.class);
        assertEquals("update", updated.get("title").asText());

        ResponseEntity<Void> deleted = restClient.delete().uri(selfHref).retrieve().toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    ResponseEntity<JsonNode> createArticle(ArticleCreateRequest request) {
        return restClient.post()
                .uri("/api/v2/articles")
                .body(request)
                .retrieve()
                .toEntity(JsonNode.class);
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
