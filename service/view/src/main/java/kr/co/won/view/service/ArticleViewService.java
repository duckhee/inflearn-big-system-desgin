package kr.co.won.view.service;

import kr.co.won.view.repository.RedisArticleViewCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;

@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private final RedisArticleViewCountRepository viewCountRepository;

    private final ArticleViewCountBackUpProcessor backUpProcessor;

    /**
     * back up을 실행할 개수 정의
     */
    private static final int BACK_UP_BATCH_SIZE = 100;

    public Long increaseViewCount(Long articleId) {
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
