package kr.co.won.hotarticle.service.handler.impl;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.CommentDeletedEventPayload;
import kr.co.won.hotarticle.repository.ArticleCommentCountRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.utils.TimeCalculatorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentDeletedEventHandler implements EventHandler<CommentDeletedEventPayload> {

    private final ArticleCommentCountRepository articleCommentCountRepository;

    @Override
    public void handle(Event<CommentDeletedEventPayload> event) {
        CommentDeletedEventPayload payload = event.getPayload();
        articleCommentCountRepository.createOrUpdate(
                payload.getArticleId(),
                payload.getArticleCommentCount(),
                TimeCalculatorUtils.calculatorDurationToMidnight()
        );
    }

    @Override
    public boolean isSupport(Event<CommentDeletedEventPayload> event) {
        return EventType.COMMENT_DELETE == event.getType();
    }

    @Override
    public Long findArticleId(Event<CommentDeletedEventPayload> event) {
        return event.getPayload().getArticleId();
    }
}
