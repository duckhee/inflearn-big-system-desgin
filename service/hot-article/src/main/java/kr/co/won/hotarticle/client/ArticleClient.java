package kr.co.won.hotarticle.client;

import jakarta.annotation.PostConstruct;
import kr.co.won.hotarticle.client.response.ArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleClient {

    private RestClient restClient;

    @Value("${endpoints.kuke-board-article-service.url}")
    private String articleServiceUrl;

    @PostConstruct
    void initRestClient() {
        restClient = RestClient.create(articleServiceUrl);
    }

    /**
     * API를 이용해서 값을 가져오기
     *
     * @param articleId
     * @return
     */
    public ArticleResponse read(Long articleId) {
        try {
            return restClient.get()
                    .uri("/api/articles/{articleId}", articleId)
                    .retrieve()
                    .body(ArticleResponse.class);
        } catch (Exception exception) {
            log.error("[ArticleClient.read] articleId={}", articleId, exception);
        }
        return null;
    }

}
