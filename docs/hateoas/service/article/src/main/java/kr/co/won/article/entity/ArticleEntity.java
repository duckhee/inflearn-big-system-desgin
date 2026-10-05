package kr.co.won.article.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 게시글 Entity [원본 코드 유지]
 *
 * - JPA 매핑은 어노테이션이 아닌 META-INF/orm.xml 로 처리한다. (tbl_article)
 *   => Entity 를 순수 자바 객체로 유지하기 위한 원본 설계를 그대로 따른다.
 * - HATEOAS 전환과 무관하다. (Entity 는 API 응답으로 직접 노출하지 않고 ArticleResponse 로 변환한다)
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity {

    /** Snowflake 로 생성되는 게시글 ID (PK) */
    private Long articleId;

    private String title;

    private String content;

    /** 게시판 ID (shard key) */
    private Long boardId;

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    /**
     * 게시글 생성 정적 팩토리 메서드
     */
    public static ArticleEntity createArticle(Long articleId, String title, String content, Long boardId, Long writerId) {
        ArticleEntity article = new ArticleEntity();
        article.articleId = articleId;
        article.title = title;
        article.content = content;
        article.boardId = boardId;
        article.writerId = writerId;
        article.createdAt = LocalDateTime.now();
        article.modifiedAt = LocalDateTime.now();
        return article;
    }

    /**
     * 게시글 수정 (JPA dirty checking 으로 반영)
     */
    public void updateArticle(String title, String content) {
        this.title = title;
        this.content = content;
        this.modifiedAt = LocalDateTime.now();
    }

}
