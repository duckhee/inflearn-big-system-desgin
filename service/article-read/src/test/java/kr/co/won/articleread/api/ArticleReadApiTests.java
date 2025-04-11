package kr.co.won.articleread.api;


import kr.co.won.articleread.service.response.ArticleReadResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class ArticleReadApiTests {

    RestClient restClient = RestClient.create("http://localhost:9005");

    @DisplayName(value = "01. article read service에 대한 읽기 기능에 대한 테스트")
    @Test
    void readArticleTests() {
        ArticleReadResponse response = restClient.get()
                .uri("/api/article-read/{articleId}", 168960288432340992l) // 임의 생성한 값
                .retrieve()
                .body(ArticleReadResponse.class);

        System.out.println("response = " + response);
    }
}
