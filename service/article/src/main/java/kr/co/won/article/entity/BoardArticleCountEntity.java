package kr.co.won.article.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Entity
@Table(name = "tbl_board_article_count")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardArticleCountEntity {

    @Id
    private Long boardId; // shard key

    private Long articleCount;


    public static BoardArticleCountEntity init(Long boardId, Long articleCount) {
        BoardArticleCountEntity entity = new BoardArticleCountEntity();
        entity.boardId = boardId;
        entity.articleCount = articleCount;
        return entity;
    }
}
