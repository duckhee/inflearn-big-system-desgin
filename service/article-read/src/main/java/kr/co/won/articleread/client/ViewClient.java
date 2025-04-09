package kr.co.won.articleread.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewClient {

    private RestClient restClient;

    @Value("${endpoints.kuke-article-view-service.url}")
    private String viewServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(viewServiceUrl);
    }

    public Long articleViewCount(Long articleId) {
        try {
            return restClient.get()
                    .uri("/api/article-views/articles/{articleId}", articleId)
                    .retrieve()
                    .body(Long.class);

        } catch (Exception e) {
            log.error("[ViewClient.articleViewCount] articleId={}", articleId, e);
            return 0l;
        }
    }
}
