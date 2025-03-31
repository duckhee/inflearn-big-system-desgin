package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleLikedEventPayload;
import kr.co.won.hotarticle.repository.ArticleLikeCountRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleLikedEventHandler implements EventHandler<ArticleLikedEventPayload> {

    private final ArticleLikeCountRepository articleLikeCountRepository;

    @Override
    public void handle(Event<ArticleLikedEventPayload> event) {
        ArticleLikedEventPayload payload = event.getPayload();
        articleLikeCountRepository.createOrUpdate(payload.getArticleId(), payload.getArticleLikeCount(), TimeCalculatorUtils.calculatorDurationToMidnight());
    }

    @Override
    public boolean isSupport(Event<ArticleLikedEventPayload> event) {
        return EventType.ARTICLE_LIKE == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleLikedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
