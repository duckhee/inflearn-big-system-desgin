package kr.co.won.comment.controller;

import kr.co.won.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class InfinityCommentControllerTest {

    RestClient restClient = RestClient.create("http://localhost:9001");

    @DisplayName(value = "01. 무한 대댓글에 대한 생성 테스트")
    @Test
    void createCommentTests() {
        CommentResponse rootComment = createRestClient(new InfinityCommentCreateRequest(1l, "testing comment", null, 1l));
        CommentResponse subComment1 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment1", rootComment.getCommentPath(), 1l));
        CommentResponse subComment2 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment2", subComment1.getCommentPath(), 1l));

        System.out.println("rootComment = " + rootComment);
        System.out.println("\t subComment1 = " + subComment1);
        System.out.println("\t\tsubComment2 = " + subComment2);

    }

    @DisplayName(value = "02. 무한 대댓글에서 댓글 가져오기 테스트")
    @Test
    void readCommentTests() {
        CommentResponse rootComment = createRestClient(new InfinityCommentCreateRequest(1l, "testing comment", null, 1l));
        CommentResponse subComment1 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment1", rootComment.getCommentPath(), 1l));
        CommentResponse subComment2 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment2", subComment1.getCommentPath(), 1l));
        CommentResponse findResponse = restClient.get()
                .uri("/api/infinity-comments/{commentId}", rootComment.getCommentId())
                .retrieve()
                .body(CommentResponse.class);
        assertEquals(rootComment.getCommentId(), findResponse.getCommentId());
    }

    @DisplayName(value = "03. 무한 대댓글에서 댓글 삭제 테스트 ")
    @Test
    void removeCommentTests() {
        CommentResponse rootComment = createRestClient(new InfinityCommentCreateRequest(1l, "testing comment", null, 1l));
        CommentResponse subComment1 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment1", rootComment.getCommentPath(), 1l));
        CommentResponse subComment2 = createRestClient(new InfinityCommentCreateRequest(1l, "testing sub comment2", subComment1.getCommentPath(), 1l));
        restClient.delete()
                .uri("/api/infinity-comments/{commentId}", subComment2.getCommentId())
                .retrieve()
                .body(Void.class);

        assertThrows(IllegalArgumentException.class, () -> {
            restClient.get()
                    .uri("/api/infinity-comments/{commentId}", subComment2.getCommentId())
                    .retrieve()
                    .body(CommentResponse.class);
        });

    }

    CommentResponse createRestClient(InfinityCommentCreateRequest request) {
        CommentResponse response = restClient.post()
                .uri("/api/infinity-comments")
                .body(request)
                .retrieve()
                .body(CommentResponse.class);
        return response;
    }


    @Getter
    @AllArgsConstructor
    class InfinityCommentCreateRequest {

        private Long articleId;

        private String content;

        private String parentPath;

        private Long writerId;
    }


}