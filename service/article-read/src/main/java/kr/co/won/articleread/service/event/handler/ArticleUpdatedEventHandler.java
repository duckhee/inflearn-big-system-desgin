package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleUpdateEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleUpdatedEventHandler implements EventHandler<ArticleUpdateEventPayload> {

    private final ArticleQueryModelRepository repository;

    /**
     * update에 대한 Event 처리를 하는 함수
     *
     * @param eventPayload
     */
    @Override
    public void handle(Event<ArticleUpdateEventPayload> eventPayload) {
        ArticleUpdateEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateArticleByEvent(payload);
                    repository.updateQueryModel(articleQueryModel);
                });
    }

    @Override
    public boolean isSupported(Event<ArticleUpdateEventPayload> eventPayload) {
        return EventType.ARTICLE_UPDATE == eventPayload.getType();
    }
}
