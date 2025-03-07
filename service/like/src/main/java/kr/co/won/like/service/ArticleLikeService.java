package kr.co.won.like.service;

import kr.co.won.common.snowflake.Snowflake;
import kr.co.won.like.entity.ArticleLikeEntity;
import kr.co.won.like.repository.ArticleLikeRepository;
import kr.co.won.like.service.response.ArticleLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleLikeService {

    private final ArticleLikeRepository likeRepository;
    private final Snowflake snowflake = new Snowflake();

    /**
     * 해당 게시글에 해당 사용자가 좋아요를 했는지 찾는 기능
     *
     * @param articleId
     * @param userId
     * @return
     */
    public ArticleLikeResponse findArticleLike(Long articleId, Long userId) {
        ArticleLikeResponse articleLikeResponse = likeRepository.findByArticleIdAndUserId(articleId, userId)
                .map(ArticleLikeResponse::from)
                .orElseThrow();
        return articleLikeResponse;
    }

    @Transactional
    public void articleLike(Long articleId, Long userId) {
        ArticleLikeEntity newArticleLike = ArticleLikeEntity.create(snowflake.nextId(), articleId, userId);
        likeRepository.save(newArticleLike);
    }

    @Transactional
    public void articleUnLike(Long articleId, Long userId) {
        likeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(likeRepository::delete);
    }
}
