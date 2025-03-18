package kr.co.won.view.repository;

import io.lettuce.core.dynamic.annotation.Param;
import kr.co.won.view.entity.ViewCountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleViewCountBackUpRepository extends JpaRepository<ViewCountEntity, Long> {

    /**
     * mysql에 조회 수에 대한 값을 BackUp을 하기 위한 함수
     *
     * @param articleId
     * @param viewCount
     * @return
     */
    @Modifying
    @Query(value = "UPDATE tbl_article_view_count SET view_count = :viewCount WHERE article_id = :articleId AND view_count < :viewCount", nativeQuery = true)
    int updateViewCount(@Param("articleId") Long articleId, @Param("viewCount") Long viewCount);
}
