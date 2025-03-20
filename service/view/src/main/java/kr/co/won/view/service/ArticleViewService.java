package kr.co.won.view.service;

import kr.co.won.view.repository.ArticleViewDistributedLockRepository;
import kr.co.won.view.repository.RedisArticleViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private final RedisArticleViewCountRepository viewCountRepository;

    private final ArticleViewCountBackUpProcessor backUpProcessor;

    private final ArticleViewDistributedLockRepository lockRepository;

    /**
     * back up을 실행할 개수 정의
     */
    private static final int BACK_UP_BATCH_SIZE = 100;

    /**
     * Lock을 걸어줄 시간 설정
     */
    private static final Duration LOCK_TTL_TIME = Duration.ofMinutes(10);

    public Long increaseViewCount(Long articleId, Long userId) {

        /** Lock에 대한 획득 */
        if (!lockRepository.getLock(articleId, userId, LOCK_TTL_TIME)) {
            return viewCountRepository.getViewCountByArticleId(articleId);
        }

        Long viewCount = viewCountRepository.increaseViewCountByArticleId(articleId);
        if (viewCount % BACK_UP_BATCH_SIZE == 0) {
            backUpProcessor.backUp(articleId, viewCount);
        }
        return viewCount;
    }

    public Long findViewCountByArticleId(Long articleId) {
        return viewCountRepository.getViewCountByArticleId(articleId);
    }


}
