package kr.co.won.article.service.request;

import lombok.Getter;
import lombok.ToString;

/**
 * 게시글 수정 요청 [원본 코드 유지]
 * => v1(PUT /api/articles/{id}), v2(PUT /api/v2/articles/{id}) 공통 사용
 */
@Getter
@ToString
public class ArticleUpdateRequest {

    private String title;

    private String content;

}
