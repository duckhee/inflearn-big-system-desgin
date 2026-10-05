package kr.co.won.article.service.response;

import lombok.Getter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

/**
 * 게시판 별 게시글 수 응답 [HATEOAS 신규]
 *
 * - v1 은 Long 값을 그대로 응답하지만, HATEOAS 에서는 링크(_links)를 붙일 "객체"가 필요하다.
 *   => Long 은 단순 값이라 EntityModel 로 감싸도 HAL 출력이 어색하고,
 *      final 클래스라 methodOn(...) 링크 생성용 프록시도 만들 수 없다.
 * - boardId 를 함께 담아 응답만 보고도 어떤 게시판의 count 인지 알 수 있게 한다.
 */
@Getter
@ToString
@Relation(itemRelation = "boardArticleCount")
public class BoardArticleCountResponse {

    private Long boardId;

    /** 게시판 전체 게시글 수 (tbl_board_article_count 값) */
    private Long articleCount;

    protected BoardArticleCountResponse() {
    }

    public static BoardArticleCountResponse of(Long boardId, Long articleCount) {
        BoardArticleCountResponse response = new BoardArticleCountResponse();
        response.boardId = boardId;
        response.articleCount = articleCount;
        return response;
    }
}
