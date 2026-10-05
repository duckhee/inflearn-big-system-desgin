package kr.co.won.article.service.response;

import kr.co.won.article.entity.ArticleEntity;
import lombok.Getter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

/**
 * 게시글 응답
 *
 * [HATEOAS 변경] @Relation 추가
 * - HAL 형식에서 목록은 "_embedded.{collectionRelation}" 아래에 들어간다.
 * - @Relation 이 없으면 클래스 이름 기반으로 "_embedded.articleResponseList" 가 된다.
 * - collectionRelation = "articles" 로 고정하면 "_embedded.articles" 가 되어 v1 의 articles 필드와 의미가 이어진다.
 * - @Relation 은 HAL 직렬화 시에만 쓰이는 메타데이터이므로 v1(일반 JSON) 응답에는 영향이 없다.
 */
@Getter
@ToString
@Relation(itemRelation = "article", collectionRelation = "articles")
public class ArticleResponse {

    private Long articleId;

    private String title;

    private String content;

    /** 게시판 ID (shard key) -> 링크 생성 시 "이 게시글이 속한 게시판 목록" 링크에 사용 */
    private Long boardId;

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    public static ArticleResponse fromEntity(ArticleEntity articleEntity) {
        ArticleResponse articleResponse = new ArticleResponse();
        articleResponse.articleId = articleEntity.getArticleId();
        articleResponse.title = articleEntity.getTitle();
        articleResponse.content = articleEntity.getContent();
        articleResponse.boardId = articleEntity.getBoardId();
        articleResponse.writerId = articleEntity.getWriterId();
        articleResponse.createdAt = articleEntity.getCreatedAt();
        articleResponse.modifiedAt = articleEntity.getModifiedAt();
        return articleResponse;
    }
}
