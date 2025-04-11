package kr.co.won.articleread.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.random.RandomGenerator;

public class DataInitializer {

    RestClient articleServiceClient = RestClient.create("http://localhost:9000");
    RestClient commentServiceClient = RestClient.create("http://localhost:9001");
    RestClient likeServiceClient = RestClient.create("http://localhost:9002");
    RestClient viewServiceClient = RestClient.create("http://localhost:9003");


    @DisplayName(value = "01. article-read 데이터 초기화 테스트")
    @Test
    void initialize() {
        for (int i = 0; i < 30; i++) {
            Long articleId = createArticle(i);
            System.out.println("articleId = " + articleId);
            long commentCount = RandomGenerator.getDefault().nextLong(10);
            long likeCount = RandomGenerator.getDefault().nextLong(10);
            long viewCount = RandomGenerator.getDefault().nextLong(200);


            createComment(articleId, commentCount);

            like(articleId, likeCount);

            view(articleId, viewCount);

        }
    }

    Long createArticle(int i) {
        return articleServiceClient.post()
                .uri("/api/articles")
                .body(new ArticleCreateRequest("title", "content", 1l, 1l))
                .retrieve()
                .body(ArticleResponse.class)
                .getArticleId();
    }

    @Getter
    @AllArgsConstructor
    static class ArticleCreateRequest {
        private String title;
        private String content;
        private Long writerId;
        private Long boardId;


    }

    @Getter
    static class ArticleResponse {
        private Long articleId;
    }


    void createComment(Long articleId, long commentCount) {
        while (commentCount-- > 0) {
            CommentResponse createCommentResponse = commentServiceClient.post()
                    .uri("/api/infinity-comments")
                    .body(new CommentCreateRequest(articleId, "content" + commentCount, 1l))
                    .retrieve()
                    .body(CommentResponse.class);
            System.out.println("createCommentResponse.commentId = " + createCommentResponse.getCommentId());
        }
    }

    @Getter
    static class CommentResponse {
        private Long commentId;

        private Long parentCommentId;

        private String commentPath;

        private Long writerId;

        private String content;

        private Boolean deleted;

        private LocalDateTime createdAt;
    }

    @Getter
    @AllArgsConstructor
    static class CommentCreateRequest {
        private Long articleId;
        private String content;
        private Long writerId;
    }

    void like(Long articleId, long likeCount) {
        while (likeCount-- > 0) {
            likeServiceClient.post()
                    .uri("/api/article-like/articles/{articleId}/users/{userId}/pessimistic-lock-1", articleId, likeCount)
                    .retrieve()
                    .body(Void.class);
        }
    }

    void view(Long articleId, long viewCount) {
        while (viewCount-- > 0) {
            Long response = viewServiceClient.post()
                    .uri("/api/article-views/articles/{articleId}/users/{users}", articleId, viewCount)
                    .retrieve()
                    .body(Long.class);
            System.out.println("response = " + response);
        }
    }
}
