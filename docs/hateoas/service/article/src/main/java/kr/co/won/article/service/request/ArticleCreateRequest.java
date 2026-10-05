package kr.co.won.article.service.request;

import lombok.Getter;
import lombok.ToString;

/**
 * 게시글 생성 요청 [원본 코드 유지]
 * => v1(POST /api/articles), v2(POST /api/v2/articles) 공통 사용
 */
@Getter
@ToString
public class ArticleCreateRequest {

    private String title;

    private String content;

    private Long writerId;

    private Long boardId;

}
