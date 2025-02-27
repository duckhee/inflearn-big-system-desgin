package kr.co.won.comment.controller;

import kr.co.won.comment.service.CommentService;
import kr.co.won.comment.service.request.CommentCreateRequest;
import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public CommentPageResponse pagingCommentResponse(@RequestParam("articleId") Long articleId, @RequestParam("page") Long pageNumber, @RequestParam("size") Long pageSize) {
        return commentService.pagingComment(articleId, pageNumber, pageSize);
    }

    @GetMapping(path = "/infinity-scroll")
    public List<CommentResponse> infinityScrollComments(@RequestParam("articleId") Long articleId, @RequestParam(value = "lastParentCommentId", required = false) Long lastParentCommentId, @RequestParam(value = "lastCommentId", required = false) Long lastCommentId, @RequestParam("size") Long pageSize) {
        return commentService.infinityScrollComment(articleId, lastParentCommentId, lastCommentId, pageSize);
    }

    @GetMapping(path = "/{commentId}")
    public CommentResponse getComment(@PathVariable("commentId") Long commentId) {
        return commentService.readComment(commentId);
    }

    @PostMapping
    public CommentResponse createComment(@RequestBody CommentCreateRequest request) {
        return commentService.createComment(request);
    }

    @DeleteMapping(path = "/{commentId}")
    public void deleteComment(@PathVariable("commentId") Long commentId) {
        commentService.deleteComment(commentId);
    }
}
