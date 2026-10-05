package kr.co.won.article.repository;

import kr.co.won.article.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 Repository [원본 코드 유지]
 *
 * - HATEOAS 전환 시 쿼리는 하나도 바꾸지 않는다.
 *   => Spring Data 의 Page<T> 를 반환하는 메서드(findAll(Pageable))로 바꾸지 않은 이유:
 *      Page 쿼리는 전체 COUNT(*) 를 실행하므로, 원본의 "커버링 인덱스 + 블록 제한 count" 최적화가 사라진다.
 *   => 대신 Controller 에서 결과를 PageImpl 로 감싸 HATEOAS 에 넘긴다. (ArticleHateoasController 참고)
 */
@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

    /**
     * 페이지 번호 방식 조회 쿼리
     * => covering index(board_id, article_id) 를 타는 서브쿼리로 article_id 만 먼저 offset/limit 한 뒤 원본 row 를 join 한다.
     */
    @Query(
            value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at " +
                    "FROM (SELECT article_id FROM tbl_article WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit OFFSET :offset) t LEFT JOIN tbl_article ON tbl_article.article_id = t.article_id",
            nativeQuery = true
    )
    List<ArticleEntity> pagingQuery(@Param("boardId") Long boardId, @Param("offset") Long offset, @Param("limit") Long limit);

    /**
     * 블록 제한 count 쿼리
     * => 전체를 세지 않고 PageLimitCalculator 가 계산한 limit 까지만 센다.
     * => 이 값이 HATEOAS 응답의 page.totalElements 가 된다.
     */
    @Query(value = "SELECT COUNT(*) FROM ( SELECT article_id FROM tbl_article WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit) t", nativeQuery = true)
    Long countPage(@Param("boardId") Long boardId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 첫 요청 (cursor 없음) -> 최신 글부터 limit 개
     */
    @Query(value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at FROM tbl_article " +
            "WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit", nativeQuery = true)
    List<ArticleEntity> findAllInfinityScroll(@Param("boardId") Long boarId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 다음 요청 (cursor = lastArticleId) -> lastArticleId 보다 작은 글 limit 개
     * => HATEOAS 응답의 next 링크에는 이번 결과의 마지막 articleId 가 lastArticleId 로 들어간다.
     */
    @Query(value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at FROM tbl_article " +
            "WHERE board_id= :boardId AND article_id < :lastArticleId ORDER BY article_id DESC LIMIT :limit", nativeQuery = true)
    List<ArticleEntity> findAllInfinityScroll(@Param("boardId") Long boarId, @Param("limit") Long limit, @Param("lastArticleId") Long lastArticleId);

}
