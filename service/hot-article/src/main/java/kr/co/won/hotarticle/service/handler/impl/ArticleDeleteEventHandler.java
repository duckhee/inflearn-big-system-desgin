package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleDeletedEventPayload;
import kr.co.won.hotarticle.repository.ArticleCreatedTimeRepository;
import kr.co.won.hotarticle.repository.HotArticleListRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDeleteEventHandler implements EventHandler<ArticleDeletedEventPayload> {

    private final HotArticleListRepository hotArticleListRepository;
    private final ArticleCreatedTimeRepository articleCreatedTimeRepository;

    @Override
    public void handle(Event<ArticleDeletedEventPayload> event) {
        ArticleDeletedEventPayload payload = event.getPayload();
        articleCreatedTimeRepository.delete(payload.getArticleId());
        hotArticleListRepository.remove(payload.getArticleId(), payload.getCreatedAt());
    }

    @Override
    public boolean isSupport(Event<ArticleDeletedEventPayload> event) {
        return EventType.ARTICLE_DELETE == event.getType();
    }

    @Override
    public Long findArticleId(Event<ArticleDeletedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
