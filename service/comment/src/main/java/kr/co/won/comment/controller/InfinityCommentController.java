package kr.co.won.comment.controller;

import kr.co.won.comment.service.InfinityCommentService;
import kr.co.won.comment.service.request.InfinityCommentCreateRequest;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/infinity-comments")
@RequiredArgsConstructor
public class InfinityCommentController {

    private final InfinityCommentService commentService;

    @GetMapping(path = "/{commentId}")
    public CommentResponse getComment(@PathVariable("commentId") Long commentId) {
        return commentService.readComment(commentId);
    }

    @PostMapping
    public CommentResponse createComment(@RequestBody InfinityCommentCreateRequest request) {
        return commentService.createComment(request);
    }

    @DeleteMapping(path = "/{commentId}")
    public void deleteComment(@PathVariable("commentId") Long commentId) {
        commentService.deleteComment(commentId);
    }


}
