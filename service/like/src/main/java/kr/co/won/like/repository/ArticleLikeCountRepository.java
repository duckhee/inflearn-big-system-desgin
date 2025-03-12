package kr.co.won.like.repository;

import jakarta.persistence.LockModeType;
import kr.co.won.like.entity.ArticleLikeCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeCountRepository extends JpaRepository<ArticleLikeCountEntity, Long> {

    /**
     * SELECT FOR UPDATE 를 이용한 값 찾기
     * => 쓰기에 대한 Lock을 획득해서 사용을 한다.
     *
     * @param articleId
     * @return
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ArticleLikeCountEntity> findLockByArticleId(Long articleId);


    /**
     * 비관적인 락을 이용한 값을 증가 하는 방식으로 좋아요 수를 증가 시켜주는 함수
     *
     * @param articleId
     * @return
     */
    @Query(value = "UPDATE tbl_article_like_count SET like_count = like_count + 1 WHERE article_id = :articleId", nativeQuery = true)
    @Modifying
    int increaseLikeCount(@Param("articleId") Long articleId);

    /**
     * @param articleId
     * @return
     */
    @Query(value = "UPDATE tbl_article_like_count SET like_count = like_count - 1 WHERE article_id = :articleId", nativeQuery = true)
    @Modifying
    int decreaseLikeCount(@Param("articleId") Long articleId);

}
