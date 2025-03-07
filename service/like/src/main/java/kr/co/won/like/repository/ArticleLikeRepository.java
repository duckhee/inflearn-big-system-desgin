package kr.co.won.like.repository;

import kr.co.won.like.entity.ArticleLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLikeEntity, Long> {

    /**
     * 게시글에 대한 아이디와 사용자 아이디를 이용을 해서 좋아요 테이블에 있는지 화인을 하는 쿼리
     * => 생성된 index 를 이용하게 된다.
     *
     * @param articleId
     * @param userId
     * @return
     */
    Optional<ArticleLikeEntity> findByArticleIdAndUserId(Long articleId, Long userId);
}
