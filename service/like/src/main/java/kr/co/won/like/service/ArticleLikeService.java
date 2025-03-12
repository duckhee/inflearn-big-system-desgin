package kr.co.won.like.service;

import jakarta.persistence.LockModeType;
import kr.co.won.common.snowflake.Snowflake;
import kr.co.won.like.entity.ArticleLikeCountEntity;
import kr.co.won.like.entity.ArticleLikeEntity;
import kr.co.won.like.repository.ArticleLikeCountRepository;
import kr.co.won.like.repository.ArticleLikeRepository;
import kr.co.won.like.service.response.ArticleLikeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleLikeService {

    private final Snowflake snowflake = new Snowflake();
    private final ArticleLikeRepository likeRepository;
    private final ArticleLikeCountRepository likeCountRepository;

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

    /**
     * 현재 게시글에 좋아요 수를 가져오는 함수
     *
     * @param articleId
     * @return
     */
    public Long likeCount(Long articleId) {
        return likeCountRepository.findById(articleId)
                .map(ArticleLikeCountEntity::getLikeCount)
                .orElse(0l);
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

    /**
     * UPDATE 구문을 이용한 좋아요 수 증가 하는 방법
     *
     * @param articleId
     * @param userId
     */
    @Transactional
    public void likePessimisticLock(Long articleId, Long userId) {
        ArticleLikeEntity likeArticle = likeRepository.save(ArticleLikeEntity.create(snowflake.nextId(), articleId, userId));

        int likeCount = likeCountRepository.increaseLikeCount(articleId);
        log.info("like count : {}", likeCount);
        if (likeCount == 0) {
            /** 최초 요청 시에 update 되는 레코드가 없으므로 1로 초기화를 해준다. */
            /** 트래픽이 순식간에 몰릴 수 있는 상황에서는 유실이 될 수 있으므로, 게시글 생성 시점에 미리 0으로 초기화를 해주는 방법으로 구현을 할 수도 있다. */
            ArticleLikeCountEntity newCount = ArticleLikeCountEntity.init(articleId, 1l);
            ArticleLikeCountEntity savedCount = likeCountRepository.save(newCount);
//            log.info("[inner]saved article like user : {}, saved article like count : {}", likeArticle.toString(), savedCount);
        }
        log.info("saved article like user : {}, saved article like count : {}", likeArticle.toString(), likeCount);
    }

    /**
     * UPDATE 구문을 이용한 좋아요 수 감소 하는 방법
     *
     * @param articleId
     * @param userId
     */
    @Transactional
    public void unlikePessimisticLock(Long articleId, Long userId) {
        likeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(entity -> {
                    likeRepository.delete(entity);
                    likeCountRepository.decreaseLikeCount(articleId);
                });
    }

    /**
     * SELECT FOR UPDATE 구문을 이용한 좋아요 수 증가 하는 방법
     *
     * @param articleId
     * @param userId
     */

    @Transactional
    public void likePessimisticLock2(Long articleId, Long userId) {
        ArticleLikeEntity newArticleLike = ArticleLikeEntity.create(snowflake.nextId(), articleId, userId);
        likeRepository.save(newArticleLike);
        ArticleLikeCountEntity likeCount = likeCountRepository.findLockByArticleId(articleId)
                .orElseGet(() -> ArticleLikeCountEntity.init(articleId, 0l));
        likeCount.increaseLikeCount();
        likeCountRepository.save(likeCount);
    }

    /**
     * SELECT FOR UPDATE 구문을 이용한 좋아요 수 감소 하는 방법
     *
     * @param articleId
     * @param userId
     */
    @Transactional
    public void unlikePessimisticLock2(Long articleId, Long userId) {
        likeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(entity -> {
                    likeRepository.delete(entity);
                    ArticleLikeCountEntity likeCount = likeCountRepository.findLockByArticleId(articleId)
                            .orElseThrow();
                    likeCount.decreaseLikeCount();
                });
    }

    /**
     * OptimisticLock(낙관적 락)을 이용한 좋아요 수 증가 하는 함수
     *
     * @param articleId
     * @param userId
     */
    @Transactional(rollbackFor = {StaleObjectStateException.class, ObjectOptimisticLockingFailureException.class})
    public void likeOptimisticLock(Long articleId, Long userId) {
        ArticleLikeEntity newArticleLike = ArticleLikeEntity.create(snowflake.nextId(), articleId, userId);
        likeRepository.save(newArticleLike);

        ArticleLikeCountEntity likeCount = likeCountRepository.findById(articleId).orElseGet(() -> ArticleLikeCountEntity.init(articleId, 0l));
        likeCount.increaseLikeCount();
        log.info("like count : {}", likeCount);
        likeCountRepository.save(likeCount);

    }

    /**
     * OptimisticLock(낙관적 락)을 이용한 좋아요 수 감소 하는 함수
     *
     * @param articleId
     * @param userId
     */
    @Transactional
    public void unlikeOptimisticLock(Long articleId, Long userId) {
        likeRepository.findByArticleIdAndUserId(articleId, userId)
                .ifPresent(entity -> {
                    likeRepository.delete(entity);

                    ArticleLikeCountEntity likeCount = likeCountRepository.findById(articleId).orElseThrow();
                    likeCount.decreaseLikeCount();
                });
    }
}
