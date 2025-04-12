package kr.co.won.articleread.service.event.handler;

import kr.co.won.articleread.repository.ArticleIdListRepository;
import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.articleread.repository.BoardArticleCountRepository;
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

    private final ArticleIdListRepository articleIdListRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

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
                    ///  시점이 중요하다 -> 삭제가 되는 시점에 목록에 대한 요청이 있을 때 문제가 생길 수 있기 때문에 먼저 목록에서 삭제를 한다.
                    // 게시글 목록
                    articleIdListRepository.deleteArticleId(payload.getBoardId(), payload.getArticleId());
                    // 데이터 삭제
                    repository.deleteQueryModel(payload.getArticleId());
                    // 게시글의 수 변경
                    boardArticleCountRepository.createOrUpdateArticleCount(payload.getBoardId(), payload.getBoardArticleCount());
                });


    }

    @Override
    public boolean isSupported(Event<ArticleDeletedEventPayload> eventPayload) {
        return EventType.ARTICLE_DELETE == eventPayload.getType();
    }
}
