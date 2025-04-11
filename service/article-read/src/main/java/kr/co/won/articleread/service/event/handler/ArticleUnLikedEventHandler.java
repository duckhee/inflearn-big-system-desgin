package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleUnLikedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleUnLikedEventHandler implements EventHandler<ArticleUnLikedEventPayload> {

    private final ArticleQueryModelRepository repository;

    @Override
    public void handle(Event<ArticleUnLikedEventPayload> eventPayload) {
        ArticleUnLikedEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateLikeCountByEvent(payload);
                    repository.updateQueryModel(articleQueryModel);
                });

    }

    @Override
    public boolean isSupported(Event<ArticleUnLikedEventPayload> eventPayload) {
        return EventType.ARTICLE_UNLIKE == eventPayload.getType();
    }
}
