package kr.co.won.view.service;

import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleViewedEventPayload;
import kr.co.won.common.outboxmessagerelay.event.OutboxEventPublisher;
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
    // kafka event 발행을 위한 추가
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void backUp(Long articleId, Long viewCount) {
        int resultQuery = backUpRepository.updateViewCount(articleId, viewCount);
        if (resultQuery == 0) {
            backUpRepository.findById(articleId)
                    .ifPresentOrElse(action -> {
                    }, () -> backUpRepository.save(ViewCountEntity.Init(articleId, viewCount)));
        }

        // event 발행
        outboxEventPublisher.publish(
                EventType.ARTICLE_VIEWED,
                ArticleViewedEventPayload.builder()
                        .articleId(articleId)
                        .articleViewCount(viewCount)
                        .build(),
                articleId
        );
    }

}
