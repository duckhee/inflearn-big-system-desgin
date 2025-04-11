package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.CommentDeletedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDeletedEventHandler implements EventHandler<CommentDeletedEventPayload> {

    private final ArticleQueryModelRepository repository;

    @Override
    public void handle(Event<CommentDeletedEventPayload> eventPayload) {
        CommentDeletedEventPayload payload = eventPayload.getPayload();
        repository.readQueryModel(payload.getArticleId())
                .ifPresent(articleQueryModel -> {
                    articleQueryModel.updateCommentCountByEvent(payload);
                    repository.updateQueryModel(articleQueryModel);
                });
    }

    @Override
    public boolean isSupported(Event<CommentDeletedEventPayload> eventPayload) {
        return EventType.COMMENT_DELETE == eventPayload.getType();
    }
}
