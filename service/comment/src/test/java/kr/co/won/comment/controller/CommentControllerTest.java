package kr.co.won.comment.controller;

import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    @DisplayName(value = "04. paging comment list Tests")
    @Test
    void pagingCommentListRestClientTests() {
        CommentPageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/comments")
                        .queryParam("articleId", 1L)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(CommentPageResponse.class);
        for (CommentResponse comment : response.getComments()) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print('\t');
            }
            System.out.println("comment ID : " + comment.getCommentId());
        }
    }

    @DisplayName(value = "05. infinity scroll comment list First Call Tests")
    @Test
    void infinityScrollFirstCallCommentListRestClientTests() {
        List<CommentResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments/infinity-scroll")
                        .queryParam("articleId", 1L)
                        .queryParam("size", 5l)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {

                });
        System.out.println("first page");
        for (CommentResponse comment : response) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print('\t');
            }
            System.out.println("comment ID : " + comment.getCommentId());
        }
    }


    @DisplayName(value = "05. infinity scroll comment list second Call Tests")
    @Test
    void infinityScrollSecondCallCommentListRestClientTests() {
        List<CommentResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments/infinity-scroll")
                        .queryParam("articleId", 1L)
                        .queryParam("size", 5l)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {

                });
        System.out.println("first page");
        for (CommentResponse comment : response) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print('\t');
            }
            System.out.println("comment ID : " + comment.getCommentId());
        }
        Long lastParentCommentId = response.get(response.size() - 1).getParentCommentId();
        Long lastCommentId = response.get(response.size() - 1).getCommentId();

        System.out.println("second page");

        List<CommentResponse> response2 = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments/infinity-scroll")
                        .queryParam("articleId", 1L)
                        .queryParam("lastParentCommentId", lastParentCommentId)
                        .queryParam("lastCommentId", lastCommentId)
                        .queryParam("size", 5l)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {
                });

        for (CommentResponse comment : response2) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print('\t');
            }
            System.out.println("comment ID : " + comment.getCommentId());
        }

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