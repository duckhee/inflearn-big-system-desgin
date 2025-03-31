package kr.co.won.hotarticle.service;

import kr.co.won.hotarticle.repository.ArticleCommentCountRepository;
import kr.co.won.hotarticle.repository.ArticleLikeCountRepository;
import kr.co.won.hotarticle.repository.ArticleViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HotArticleScoreCalculator {

    private final ArticleViewCountRepository articleViewCountRepository;

    private final ArticleCommentCountRepository articleCommentCountRepository;

    private final ArticleLikeCountRepository articleLikeCountRepository;

    // 각 count에 대한 점수를 계산할 때 사용을 할 가중치
    private final static long ARTICLE_LIKE_COUNT_WEIGHT = 3;
    private final static long ARTICLE_COMMENT_COUNT_WEIGHT = 2;
    private final static long ARTICLE_VIEW_COUNT_WEIGHT = 1;

    public long calculate(Long articleId) {
        Long articleLikeCount = articleLikeCountRepository.read(articleId);
        Long articleViewCount = articleViewCountRepository.read(articleId);
        Long articleCommentCount = articleCommentCountRepository.read(articleId);

        return (articleLikeCount * ARTICLE_LIKE_COUNT_WEIGHT
                + articleCommentCount * ARTICLE_COMMENT_COUNT_WEIGHT
                + articleViewCount * ARTICLE_VIEW_COUNT_WEIGHT);
    }
}
