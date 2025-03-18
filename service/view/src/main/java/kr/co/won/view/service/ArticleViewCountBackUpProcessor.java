package kr.co.won.view.service;

import kr.co.won.view.entity.ViewCountEntity;
import kr.co.won.view.repository.ArticleViewCountBackUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ArticleViewCountBackUpProcessor {

    private final ArticleViewCountBackUpRepository backUpRepository;

    @Transactional
    public void backUp(Long articleId, Long viewCount) {
        int resultQuery = backUpRepository.updateViewCount(articleId, viewCount);
        if (resultQuery == 0) {
            backUpRepository.findById(articleId)
                    .ifPresentOrElse(action -> {
                    }, () -> backUpRepository.save(ViewCountEntity.Init(articleId, viewCount)));
        }
    }

}
