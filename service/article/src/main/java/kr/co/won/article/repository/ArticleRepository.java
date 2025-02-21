package kr.co.won.article.repository;

import kr.co.won.article.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<ArticleEntity, Long> {

    /**
     * paging 을 위한 쿼리 -> covering index 를 이용하기 위한 직접 쿼리 사용
     * sub query 를 이용을 해서 해당 값을 선택을 한 후 가져온다.
     */
    @Query(
            value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at " +
                    "FROM (SELECT article_id FROM tbl_article WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit OFFSET :offset) t LEFT JOIN tbl_article ON tbl_article.article_id = t.article_id",
            nativeQuery = true
    )
    List<ArticleEntity> pagingQuery(@Param("boardId") Long boardId, @Param("offset") Long offset, @Param("limit") Long limit);

    /**
     * 전체 페이지의 수를 카운트 하는 쿼리
     */
    @Query(value = "SELECT COUNT(*) FROM ( SELECT article_id FROM tbl_article WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit) t", nativeQuery = true)
    Long countPage(@Param("boardId") Long boardId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 처음 요청 시 처리를 하는 쿼리
     *
     * @param boarId
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at FROM tbl_article " +
            "WHERE board_id= :boardId ORDER BY article_id DESC LIMIT :limit", nativeQuery = true)
    List<ArticleEntity> findAllInfinityScroll(@Param("boardId") Long boarId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 기준점이 있을 때 요청 시 처리를 하는 쿼리
     *
     * @param boarId
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_article.article_id, tbl_article.title, tbl_article.content, tbl_article.board_id, tbl_article.writer_id, tbl_article.created_at, tbl_article.modified_at FROM tbl_article " +
            "WHERE board_id= :boardId AND article_id < :lastArticleId ORDER BY article_id DESC LIMIT :limit", nativeQuery = true)
    List<ArticleEntity> findAllInfinityScroll(@Param("boardId") Long boarId, @Param("limit") Long limit, @Param("lastArticleId") Long lastArticleId);

}
