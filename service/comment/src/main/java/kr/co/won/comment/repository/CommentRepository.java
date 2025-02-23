package kr.co.won.comment.repository;

import kr.co.won.comment.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
