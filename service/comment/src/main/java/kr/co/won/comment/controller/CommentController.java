package kr.co.won.comment.controller;

import kr.co.won.comment.service.CommentService;
import kr.co.won.comment.service.request.CommentCreateRequest;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

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
