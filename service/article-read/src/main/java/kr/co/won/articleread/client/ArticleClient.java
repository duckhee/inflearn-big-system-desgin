package kr.co.won.articleread.client;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleClient {

    private RestClient restClient;

    @Value("${endpoints.kuke-board-article-service.url}")
    private String articleServiceUrl;

    @PostConstruct
    public void initRestClient() {
        restClient = RestClient.create(articleServiceUrl);
    }


    public Optional<ArticleResponse> readArticle(Long articleId) {
        try {
            ArticleResponse response = restClient.get()
                    .uri("/api/articles/{articleId}", articleId)
                    .retrieve()
                    .body(ArticleResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("[ArticleClient.readArticle] articleId={}", articleId, e);
            return Optional.empty();
        }
    }

    /**
     * Paging 방식의 게시글 목록 요청 처리 함수
     *
     * @param boardId
     * @param page
     * @param pageSize
     * @return
     */
    public ArticlePageResponse pagingArticleListResponse(Long boardId, Long page, Long pageSize) {
        try {
            return restClient.get()
                    .uri(uriBuilder ->
                            uriBuilder.path("/api/articles")
                                    .queryParam("boardId", boardId)
                                    .queryParam("page", page)
                                    .queryParam("size", pageSize).build()
                    )
                    .retrieve()
                    .body(ArticlePageResponse.class);
        } catch (Exception e) {
            log.error("[ArticleClient.pagingArticleListResponse] boardId={}, page={}, pageSize={}", boardId, page, pageSize, e);
            return ArticlePageResponse.EMPTY;
        }
    }

    /**
     * 무한 스크롤 방식의 게시글 페이지 목록 요청 처리 함수
     *
     * @param boardId
     * @param lastArticleId
     * @param limit
     * @return
     */
    public List<ArticleResponse> infinityScrollArticleListResponse(Long boardId, Long lastArticleId, Long limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        if (lastArticleId != null) {
                            return uriBuilder.path("/api/articles/infinity-scroll")
                                    .queryParam("boardId", boardId)
                                    .queryParam("size", limit)
                                    .queryParam("lastArticleId", lastArticleId)
                                    .build();
                        } else {
                            return uriBuilder.path("/api/articles/infinity-scroll")
                                    .queryParam("boardId", boardId)
                                    .queryParam("size", limit)
                                    .build();
                        }
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ArticleResponse>>() {
                    });
        } catch (Exception e) {
            log.error("[ArticleClient.infinityScrollArticleListResponse] boardId={}, lastArticleId={}, limit={}", boardId, lastArticleId, limit, e);
            return List.of();
        }
    }

    public long countArtice(Long boardId) {
        try {
            return restClient.get()
                    .uri("api/articles/boards/{boardId}/article-count", boardId)
                    .retrieve()
                    .body(Long.class);
        } catch (Exception e) {
            log.error("[ArticleClient.countArticle] boardId={}", boardId, e);
            return 0l;
        }
    }

    /**
     * 게시글 서비스에 대한 응답 클래스
     */
    @Getter
    public static class ArticleResponse {
        private Long articleId;
        private String title;
        private String content;
        private Long boardId;
        private Long writerId;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
    }

    /**
     * 서비스 목록에 대한 조회 결과
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticlePageResponse {
        private List<ArticleResponse> articles;

        private Long articleCount;
        public static ArticlePageResponse EMPTY = new ArticlePageResponse(List.of(), 0l);
    }
}
