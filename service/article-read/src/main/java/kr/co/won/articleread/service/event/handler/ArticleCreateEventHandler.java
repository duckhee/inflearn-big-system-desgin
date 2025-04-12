package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleIdListRepository;
import kr.co.won.articleread.repository.ArticleQueryModel;
import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.articleread.repository.BoardArticleCountRepository;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleCreateEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCreateEventHandler implements EventHandler<ArticleCreateEventPayload> {

    private final ArticleQueryModelRepository repository;

    private final ArticleIdListRepository articleIdListRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

    @Override
    public void handle(Event<ArticleCreateEventPayload> eventPayload) {
        ArticleCreateEventPayload payload = eventPayload.getPayload();
        repository.saveQueryModel(ArticleQueryModel.create(payload), Duration.ofDays(1));
        // 게시글 목록
        articleIdListRepository.addArticleIdList(payload.getBoardId(), payload.getArticleId(), 1000l);
        boardArticleCountRepository.createOrUpdateArticleCount(payload.getBoardId(), payload.getBoardArticleCount());

    }

    @Override
    public boolean isSupported(Event<ArticleCreateEventPayload> eventPayload) {
        return EventType.ARTICLE_CREATE == eventPayload.getType();
    }
}
