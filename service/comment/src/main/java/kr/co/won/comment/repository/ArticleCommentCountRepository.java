package kr.co.won.comment.repository;

import kr.co.won.comment.entity.ArticleCommentCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleCommentCountRepository extends JpaRepository<ArticleCommentCountEntity, Long> {

    /**
     * comment의 수를 증가를 해주는 함수
     * => Pessimistic Lock을 이용해서 값을 증가 해준다.
     *
     * @param articleId
     * @return
     */
    @Modifying
    @Query(value = "UPDATE tbl_article_comment_count SET comment_count = comment_count + 1 WHERE article_id = :articleId", nativeQuery = true)
    int increaseCommentCount(@Param("articleId") Long articleId);


    /**
     * comment의 수를 감소를 해주는 함수
     * => 비관적인 락을 이용해서 값을 감소 해준다.
     *
     * @param articleId
     * @return
     */
    @Modifying
    @Query(value = "UPDATE tbl_article_comment_count SET comment_count = comment_count -1 WHERE article_id = :articleId", nativeQuery = true)
    int decreaseCommentCount(@Param("articleId") Long articleId);
}
