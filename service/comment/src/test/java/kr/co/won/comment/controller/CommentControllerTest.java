package kr.co.won.comment.controller;

import kr.co.won.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;


class CommentControllerTest {

    private RestClient restClient = RestClient.create("http://localhost:9001");

    @DisplayName(value = "01. create comment Tests")
    @Test
    void createCommentRestClientTests() {
        CommentResponse commentCreateResponse1 = createCommentResponse(new CommentCreateRequest(1l, "my comment1", null, 1l));
        CommentResponse commentCreateResponse2 = createCommentResponse(new CommentCreateRequest(1l, "my comment2", commentCreateResponse1.getCommentId(), 1l));
        CommentResponse commentCreateResponse3 = createCommentResponse(new CommentCreateRequest(1l, "my comment3", commentCreateResponse1.getCommentId(), 1l));

        System.out.println("commentCreateResponse1 = " + commentCreateResponse1);
        System.out.println("commentCreateResponse2 = " + commentCreateResponse2);
        System.out.println("commentCreateResponse3 = " + commentCreateResponse3);

    }

    @DisplayName(value = "02. read comment Tests")
    @Test
    void readCommentRestClientTests() {
        CommentResponse commentCreateResponse1 = createCommentResponse(new CommentCreateRequest(1l, "my comment1", null, 1l));
        CommentResponse readCommentResponse = restClient.get()
                .uri("/api/comments/{commentId}", commentCreateResponse1.getCommentId())
                .retrieve()
                .body(CommentResponse.class);

        assertEquals(commentCreateResponse1.getCommentId(), readCommentResponse.getCommentId());
    }

    @DisplayName(value = "03. comment Delete Tests")
    @Test
    void deleteCommentRestClientTests() {
        CommentResponse commentCreateResponse1 = createCommentResponse(new CommentCreateRequest(1l, "my comment1", null, 1l));
        CommentResponse commentCreateResponse2 = createCommentResponse(new CommentCreateRequest(1l, "my comment2", commentCreateResponse1.getCommentId(), 1l));
        CommentResponse commentCreateResponse3 = createCommentResponse(new CommentCreateRequest(1l, "my comment3", commentCreateResponse1.getCommentId(), 1l));
        /** root comment */
        restClient.delete()
                .uri("/api/comments/{commentId}", commentCreateResponse1.getCommentId())
                .retrieve().body(Void.class);

        CommentResponse deleteCommentResponse = restClient.get()
                .uri("/api/comments/{commentId}", commentCreateResponse1.getCommentId())
                .retrieve().body(CommentResponse.class);
        assertTrue(deleteCommentResponse.getDeleted());

        /** not root comment */
        restClient.delete()
                .uri("/api/comments/{commentId}", commentCreateResponse2.getCommentId())
                .retrieve().body(Void.class);

        /** 데이터가 삭제가 되었기 때문에 조회가 되지 않는다. */
        Assertions.assertThrows(Exception.class, () -> {
            restClient.get()
                    .uri("/api/comments/{commentId}", commentCreateResponse2.getCommentId())
                    .retrieve().body(CommentResponse.class);
        });

        /** not root comment */
        restClient.delete()
                .uri("/api/comments/{commentId}", commentCreateResponse3.getCommentId())
                .retrieve().body(Void.class);

        /** 데이터가 삭제가 되었기 때문에 조회가 되지 않는다. */
        Assertions.assertThrows(Exception.class, () -> {
            restClient.get()
                    .uri("/api/comments/{commentId}", commentCreateResponse3.getCommentId())
                    .retrieve().body(CommentResponse.class);
        });

        /** 자식에 대한 댓9글이 모두 삭제가 되어서 루트 댓글도 삭제가 된다. */
        Assertions.assertThrows(Exception.class, () -> {
            restClient.get()
                    .uri("/api/comments/{commentId}", commentCreateResponse1.getCommentId())
                    .retrieve().body(CommentResponse.class);
        });

    }


    CommentResponse createCommentResponse(CommentCreateRequest request) {
        return restClient.post()
                .uri("/api/comments")
                .body(request)
                .retrieve()
                .body(CommentResponse.class);
    }


    @Getter
    @AllArgsConstructor
    public static class CommentCreateRequest {

        private Long articleId;

        private String content;

        private Long parentCommentId;

        private Long writerId;

    }

}