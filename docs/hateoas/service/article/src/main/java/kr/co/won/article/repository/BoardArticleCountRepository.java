package kr.co.won.article.repository;

import kr.co.won.article.entity.BoardArticleCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 게시판 별 게시글 수 Repository [원본 코드 유지]
 */
@Repository
public interface BoardArticleCountRepository extends JpaRepository<BoardArticleCountEntity, Long> {

    /**
     * 게시글 수 1 증가
     * => UPDATE 문 자체의 row lock 으로 동시성을 보장한다. (반환값 0 이면 row 가 없으므로 Service 에서 insert)
     */
    @Modifying
    @Query(value = "UPDATE tbl_board_article_count SET article_count=article_count + 1 WHERE board_id = :boardId ", nativeQuery = true)
    int increaseArticleCount(@Param("boardId") Long articleId);

    /**
     * 게시글 수 1 감소
     */
    @Modifying
    @Query(value = "UPDATE tbl_board_article_count SET article_count = article_count - 1 WHERE board_id = :boardId", nativeQuery = true)
    int decreaseArticleCount(@Param("boardId") Long boardId);

}
