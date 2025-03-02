package kr.co.won.comment.service.request;

import lombok.Getter;

@Getter
public class InfinityCommentCreateRequest {

    private Long articleId;

    private String content;

    private String parentPath;

    private Long writerId;
}
