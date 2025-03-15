package kr.co.won.article.repository;

import kr.co.won.article.entity.BoardArticleCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardArticleCountRepository extends JpaRepository<BoardArticleCountEntity, Long> {

    /**
     * 게시글에 대한 수를 1 증가를 해주는 함수
     * => 비관적인 락을 이용해서 수정을 하는 쿼리를 사용을 한다.
     *
     * @param articleId
     * @return
     */
    @Modifying
    @Query(value = "UPDATE tbl_board_article_count SET article_count=article_count + 1 WHERE board_id = :boardId ", nativeQuery = true)
    int increaseArticleCount(@Param("boardId") Long articleId);

    /**
     * 게시글에 대한 수를 1 감소를 해주는 함수
     * => 비관적인 락을 이용해서 수정을 하는 쿼리를 사용을 한다.
     *
     * @param boardId
     * @return
     */
    @Modifying
    @Query(value = "UPDATE tbl_board_article_count SET article_count = article_count - 1 WHERE board_id = :boardId", nativeQuery = true)
    int decreaseArticleCount(@Param("boardId") Long boardId);

}
