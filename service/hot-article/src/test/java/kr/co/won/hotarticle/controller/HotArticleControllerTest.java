package kr.co.won.hotarticle.controller;

import kr.co.won.hotarticle.service.response.HotArticleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HotArticleControllerTest {

    RestClient hotArticleClient = RestClient.create("http://localhost:9004");

    @DisplayName("01. 인기글에 대해서 모두 읽어오는 API 테스트")
    @Test
    void readAllHotArticleTests() {
        System.out.println("hot article readAllHotArticleTests");
        List<HotArticleResponse> response = hotArticleClient.get()
                .uri("/api/hot-articles/articles/date/{dateStr}", "20250407")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        for (HotArticleResponse hotArticleResponse : response) {
            System.out.println("hotArticleResponse = " + hotArticleResponse.toString());
        }

    }
}