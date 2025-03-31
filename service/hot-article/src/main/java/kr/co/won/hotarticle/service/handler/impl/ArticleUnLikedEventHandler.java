package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleUnLikedEventPayload;
import kr.co.won.hotarticle.repository.ArticleLikeCountRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleUnLikedEventHandler implements EventHandler<ArticleUnLikedEventPayload> {

    private final ArticleLikeCountRepository articleLikeCountRepository;

    @Override
    public void handle(Event<ArticleUnLikedEventPayload> event) {
        ArticleUnLikedEventPayload payload = event.getPayload();
        articleLikeCountRepository.createOrUpdate(payload.getArticleId(), payload.getArticleLikeCount(), TimeCalculatorUtils.calculatorDurationToMidnight());
    }

    @Override
    public boolean isSupport(Event<ArticleUnLikedEventPayload> event) {
        return EventType.ARTICLE_UNLIKE == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleUnLikedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
