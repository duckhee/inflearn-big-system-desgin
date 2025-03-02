package kr.co.won.comment.repository;

import kr.co.won.comment.entity.InfinityCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfinityCommentRepository extends JpaRepository<InfinityCommentEntity, Long> {

    /**
     * Path를 가지고 comment 를 가져오기
     *
     * @param path
     * @return
     */
    @Query(value = "SELECT c FROM InfinityCommentEntity c WHERE c.commentPath.path = :path")
    Optional<InfinityCommentEntity> findByPath(@Param("path") String path);


    /**
     * Path에 대한 부모 댓글의 Path를 가지고 부모 댓글이 아닌 댓글의 최대 값 가져오는 쿼리 -> Path에 대한 규칙을 통해서 부모 댓글의 Path에 대해서 가져올 수 있기 때문에 자식의 최대 값을 통해 다음 댓글의 순서를 알 수 있고 부모 댓글의 path를 찾을 수 있다.
     *
     * @param articleId
     * @param pathPrefix
     * @return
     */
    @Query(value = "SELECT path FROM tbl_infinity_comment " +
            "WHERE article_id = :articleId " +
            "AND path > :pathPrefix " +
            "AND path LIKE :pathPrefix% " +
            "ORDER BY path DESC LIMIT 1", nativeQuery = true)
    Optional<String> findDescendantTopPath(@Param("articleId") Long articleId, @Param("pathPrefix") String pathPrefix);


}
