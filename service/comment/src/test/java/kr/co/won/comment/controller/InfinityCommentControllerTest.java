package kr.co.won.comment.controller;

import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

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

        assertThrows(Exception.class, () -> {
            restClient.get()
                    .uri("/api/infinity-comments/{commentId}", subComment2.getCommentId())
                    .retrieve()
                    .body(CommentResponse.class);
        });
    }

    @DisplayName(value = "04. 페이징 방식으로 페이지 가져오기 테스트")
    @Test
    void pagingInfinityCommentsTests() {
        CommentPageResponse pagingResponse = restClient.get()
                .uri(builder -> builder.path("/api/infinity-comments")
                        .queryParam("articleId", 1)
                        .queryParam("size", 10)
                        .queryParam("page", 1)
                        .build())
                .retrieve()
                .body(CommentPageResponse.class);
        assertEquals(pagingResponse.getComments().size(), 10);
    }

    CommentResponse createRestClient(InfinityCommentCreateRequest request) {
        CommentResponse response = restClient.post()
                .uri("/api/infinity-comments")
                .body(request)
                .retrieve()
                .body(CommentResponse.class);
        return response;
    }

    @DisplayName(value = "05. 무한 스크롤 방식으로 댓글 가져오기 테스트 -> 처음 요청 ")
    @Test
    void infinityScrollFirstCommentsTests() {
        List<CommentResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/infinity-comments/infinity-scroll")
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

    @DisplayName(value = "05. 무한 스크롤 방식으로 댓글 가져오기 테스트 -> 두번째 ")
    @Test
    void infinityScrollWithLastPathCommentsTests() {
        List<CommentResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/infinity-comments/infinity-scroll")
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

        CommentResponse lastComment = response.get(response.size() - 1);

        List<CommentResponse> responseWitLastPath = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/infinity-comments/infinity-scroll")
                        .queryParam("articleId", 1L)
                        .queryParam("size", 5l)
                        .queryParam("lastPath", lastComment.getCommentPath())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CommentResponse>>() {

                });
        System.out.println("comment with last path");
        for (CommentResponse comment : responseWitLastPath) {
            if (!comment.getCommentId().equals(comment.getParentCommentId())) {
                System.out.print('\t');
            }
            System.out.println("comment ID : " + comment.getCommentId());
        }
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