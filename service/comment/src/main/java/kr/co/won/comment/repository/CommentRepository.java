package kr.co.won.comment.repository;

import kr.co.won.comment.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    /**
     * 해당 게시글에 하위 게시글이 있는지 확인을 하기 위한 쿼리
     * => 부모에 대한 값이 없을 때 자기 자신의 아이디를 넣어주기 때문에 하위 댓글이 있는지 없는지 확인을 할 때 2가 나와야 자식 댓글이 있다고 판단이 된다.
     *
     * @param articleId
     * @param parentCommentId
     * @param limit
     * @return
     */
    @Query(value = "SELECT count(*) FROM (" +
            "SELECT comment_id FROM tbl_comment WHERE article_id = :articleId AND parent_comment_id = :parentCommentId LIMIT :limit) subComment", nativeQuery = true)
    Long checkSubComment(@Param("articleId") Long articleId, @Param("parentCommentId") Long parentCommentId, @Param("limit") Long limit);

    /**
     * 게시글에 대한 댓글 목록을 페이징 방식으로 가져오기 위한 댓글 목록 페이징 쿼리
     * => 서브 쿼리를 이용을 해서 인덱스에서 PK를 가져온 다음에 해당 PK를 JOIN을 이용을 해서 해당이 되는 데이터를 가져온다.
     *
     * @param articleId
     * @param offset
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_comment.comment_id, tbl_comment.content, tbl_comment.parent_comment_id, tbl_comment.article_id, tbl_comment.writer_id, tbl_comment.deleted, tbl_comment.created_at " +
            "FROM (SELECT comment_id FROM tbl_comment WHERE article_id = :articleId ORDER BY parent_comment_id ASC, comment_id ASC LIMIT :limit OFFSET :offset) t LEFT JOIN tbl_comment ON t.comment_id = tbl_comment.comment_id", nativeQuery = true)
    List<CommentEntity> pagingComment(@Param("articleId") Long articleId, @Param("offset") Long offset, @Param("limit") Long limit);

    /**
     * 게시글에 대한 댓글의 페이지 개수를 파악하기 위한 게시글의 수를 가져오는 쿼리
     * => 서브 쿼리를 이용을 해서 PK 값만 가져오도록 제한을 하고 해당 값의 개수를 반환을 하는 함수이다.
     *
     * @param articleId
     * @param limit
     * @return
     */
    @Query(value = "SELECT COUNT(*) " +
            "FROM (SELECT comment_id FROM tbl_comment WHERE article_id = :articleId LIMIT :limit) subComment", nativeQuery = true)
    Long commentPagingNumberCount(@Param("articleId") Long articleId, @Param("limit") Long limit);

    /**
     * 무한 스크롤 형식의 댓글 목록 요청에 대한 기준점이 없을 때 요청 쿼리
     * => 무한 스크롤 시 기준점이 없을 때 요청을 하는 쿼리이다.
     *
     * @param articleId
     * @param limit
     * @return
     */
    @Query(value = "SELECT tbl_comment.comment_id, tbl_comment.content, tbl_comment.parent_comment_id, tbl_comment.article_id, tbl_comment.writer_id, tbl_comment.deleted, tbl_comment.created_at " +
            "FROM tbl_comment WHERE article_id = :articleId ORDER BY parent_comment_id ASC, comment_id ASC LIMIT :limit", nativeQuery = true)
    List<CommentEntity> infinityScrollComment(@Param("articleId") Long articleId, @Param("limit") Long limit);

    @Query(value = "SELECT tbl_comment.comment_id, tbl_comment.content, tbl_comment.parent_comment_id, tbl_comment.article_id, tbl_comment.writer_id, tbl_comment.deleted, tbl_comment.created_at " +
            "FROM tbl_comment " +
            "WHERE article_id = :articleId " +
            "AND (parent_comment_id > :lastParentCommentId OR (parent_comment_id = :lastParentCommentId AND comment_id > :lastCommentId))" +
            "ORDER BY parent_comment_id ASC, comment_id ASC LIMIT :limit", nativeQuery = true)
    List<CommentEntity> infinityScrollComment(@Param("articleId") Long articleId, @Param("lastParentCommentId") Long parentCommentId, @Param("lastCommentId") Long commentId, @Param("limit") Long limit);
}
