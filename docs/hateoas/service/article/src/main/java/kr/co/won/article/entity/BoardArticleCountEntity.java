package kr.co.won.article.entity;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 게시판 별 게시글 수 Entity [원본 코드 유지]
 *
 * - 게시판 전체 게시글 수를 매번 COUNT(*) 하지 않기 위해 별도 테이블(tbl_board_article_count)에 보관한다.
 * - JPA 매핑은 META-INF/orm.xml 로 처리한다.
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardArticleCountEntity {

    /** 게시판 ID (shard key) */
    @Id
    private Long boardId;

    private Long articleCount;


    public static BoardArticleCountEntity init(Long boardId, Long articleCount) {
        BoardArticleCountEntity entity = new BoardArticleCountEntity();
        entity.boardId = boardId;
        entity.articleCount = articleCount;
        return entity;
    }
}
