package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleDeletedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleDeletedEventHandler implements EventHandler<ArticleDeletedEventPayload> {

    private final ArticleQueryModelRepository repository;

    /**
     * 게시글에 대해서 삭제를 하는 함수
     *
     * @param eventPayload
     */
    @Override
    public void handle(Event<ArticleDeletedEventPayload> eventPayload) {
        ArticleDeletedEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    repository.deleteQueryModel(payload.getArticleId());
                });

    }

    @Override
    public boolean isSupported(Event<ArticleDeletedEventPayload> eventPayload) {
        return EventType.ARTICLE_DELETE == eventPayload.getType();
    }
}
