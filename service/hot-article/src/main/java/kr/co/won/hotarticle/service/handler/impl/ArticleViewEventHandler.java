package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleViewedEventPayload;
import kr.co.won.hotarticle.repository.ArticleViewCountRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleViewEventHandler implements EventHandler<ArticleViewedEventPayload> {

    private final ArticleViewCountRepository articleViewCountRepository;

    @Override
    public void handle(Event<ArticleViewedEventPayload> event) {
        ArticleViewedEventPayload payload = event.getPayload();
        articleViewCountRepository.createOrUpdate(
                payload.getArticleId(),
                payload.getArticleViewCount(),
                TimeCalculatorUtils.calculatorDurationToMidnight()
        );
    }

    @Override
    public boolean isSupport(Event<ArticleViewedEventPayload> event) {
        return EventType.ARTICLE_VIEWED == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleViewedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
