package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleCreateEventPayload;
import kr.co.won.hotarticle.repository.ArticleCreatedTimeRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCreatedEventHandler implements EventHandler<ArticleCreateEventPayload> {

    private final ArticleCreatedTimeRepository articleCreatedTimeRepository;

    @Override
    public void handle(Event<ArticleCreateEventPayload> event) {
        ArticleCreateEventPayload payload = event.getPayload();
        articleCreatedTimeRepository.createOrUpdate(payload.getArticleId(), payload.getCreatedAt(), TimeCalculatorUtils.calculatorDurationToMidnight());
    }

    @Override
    public boolean isSupport(Event<ArticleCreateEventPayload> event) {
        return EventType.ARTICLE_CREATE == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleCreateEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
