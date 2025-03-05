package kr.co.won.comment.controller;

import kr.co.won.comment.service.InfinityCommentService;
import kr.co.won.comment.service.request.InfinityCommentCreateRequest;
import kr.co.won.comment.service.response.CommentPageResponse;
import kr.co.won.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/infinity-comments")
@RequiredArgsConstructor
public class InfinityCommentController {

    private final InfinityCommentService commentService;

    @GetMapping
    public CommentPageResponse pagingComment(@RequestParam(name = "articleId") Long articleId, @RequestParam("page") Long pageNumber, @RequestParam("size") Long pageSize) {
        return commentService.pagingComments(articleId, pageNumber, pageSize);
    }

    @GetMapping(path = "/infinity-scroll")
    public List<CommentResponse> infinityScrollComments(@RequestParam("articleId") Long articleId, @RequestParam(name = "lastPath", required = false) String lastPath, @RequestParam("size") Long pageSize) {
        return commentService.infinityScrollComments(articleId, lastPath, pageSize);
    }

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
