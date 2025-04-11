package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleLikedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleLikedEventHandler implements EventHandler<ArticleLikedEventPayload> {

    private final ArticleQueryModelRepository repository;

    @Override
    public void handle(Event<ArticleLikedEventPayload> eventPayload) {
        ArticleLikedEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateLikeCountByEvent(payload);
                    repository.updateQueryModel(articleQueryModel);
                });
    }

    @Override
    public boolean isSupported(Event<ArticleLikedEventPayload> eventPayload) {
        return EventType.ARTICLE_LIKE == eventPayload.getType();
    }
}
