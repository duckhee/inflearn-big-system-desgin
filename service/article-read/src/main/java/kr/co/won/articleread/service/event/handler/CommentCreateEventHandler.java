package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.CommentCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCreateEventHandler implements EventHandler<CommentCreatedEventPayload> {

    private final ArticleQueryModelRepository repository;

    @Override
    public void handle(Event<CommentCreatedEventPayload> eventPayload) {
        CommentCreatedEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateCommentCountByEvent(payload);
                    repository.updateQueryModel(articleQueryModel);
                });
    }

    @Override
    public boolean isSupported(Event<CommentCreatedEventPayload> eventPayload) {
        return EventType.COMMENT_CREATE == eventPayload.getType();
    }
}
