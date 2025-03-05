package kr.co.won.comment.repository;

import kr.co.won.comment.entity.InfinityCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    @Query(value = "SELECT path FROM tbl_infinity_comments " +
            "WHERE article_id = :articleId " +
            "AND path > :pathPrefix " +
            "AND path LIKE :pathPrefix% " +
            "ORDER BY path DESC LIMIT 1", nativeQuery = true)
    Optional<String> findDescendantTopPath(@Param("articleId") Long articleId, @Param("pathPrefix") String pathPrefix);


    /**
     * Paging 방식으로 댓글에 대한 목록을 가져오는 함수
     * => SUB QUERY를 이용을 해서 PK 값을 선택을 한 뒤에 LEFT JOIN을 통해서 값을 가져온다.
     *
     * @param articleId
     * @param offset
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_infinity_comments.comment_id, tbl_infinity_comments.content, tbl_infinity_comments.path, tbl_infinity_comments.article_id, tbl_infinity_comments.writer_id, tbl_infinity_comments.deleted, tbl_infinity_comments.created_at " +
            "FROM (SELECT comment_id FROM tbl_infinity_comments " +
            "WHERE article_id = :articleId ORDER BY path ASC LIMIT :limit OFFSET :offset) t LEFT JOIN tbl_infinity_comments ON t.comment_id=tbl_infinity_comments.comment_id", nativeQuery = true)
    List<InfinityCommentEntity> pagingComments(@Param("articleId") Long articleId, @Param("offset") Long offset, @Param("limit") Long limit);

    /**
     * paging 방식을 사용을 할 때 페이지에 대한 개수를 가져오기 위한 쿼리
     *
     * @param articleId
     * @param limit
     * @return
     */
    @Query(value = "SELECT count(*) FROM (SELECT comment_id FROM tbl_infinity_comments WHERE article_id= :articleId LIMIT :limit) SubComment", nativeQuery = true)
    Long commentPagingNumber(@Param("articleId") Long articleId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 형태로 목록을 가져올 때 사용을 하는 쿼리 함수
     * => 처음 요청이기 때문에 기준이 되는 값을 인자로 받지 않는다.
     *
     * @param articleId
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_infinity_comments.comment_id, tbl_infinity_comments.content, tbl_infinity_comments.path, tbl_infinity_comments.article_id, tbl_infinity_comments.writer_id, tbl_infinity_comments.deleted, tbl_infinity_comments.created_at " +
            "FROM tbl_infinity_comments " +
            "WHERE article_id= :articleId ORDER BY path ASC limit :limit ", nativeQuery = true)
    List<InfinityCommentEntity> infinityCommentsScroll(@Param("articleId") Long articleId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 형태로 록록을 가져올 때 사용을 하는 함수
     * => 두번째 요청 부터 생기는 기준점을 가지고 쿼리를 사용한다.
     *
     * @param articleId
     * @param path
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_infinity_comments.comment_id, tbl_infinity_comments.content, tbl_infinity_comments.path, tbl_infinity_comments.article_id, tbl_infinity_comments.writer_id, tbl_infinity_comments.deleted, tbl_infinity_comments.created_at FROM tbl_infinity_comments " +
            "WHERE article_id= :articleId " +
            "AND path > :lastPath " +
            "ORDER BY path ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<InfinityCommentEntity> infinityCommentsScroll(@Param("articleId") Long articleId, @Param("lastPath") String path, @Param("limit") Long limit);

}
