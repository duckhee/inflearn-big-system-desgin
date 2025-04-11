package kr.co.won.articleread.service;

import kr.co.won.articleread.client.ArticleClient;
import kr.co.won.articleread.client.CommentClient;
import kr.co.won.articleread.client.LikeClient;
import kr.co.won.articleread.client.ViewClient;
import kr.co.won.articleread.repository.ArticleQueryModel;
import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.articleread.service.event.handler.EventHandler;
import kr.co.won.articleread.service.response.ArticleReadResponse;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReadService {

    private final ArticleClient articleClient;
    private final CommentClient commentClient;
    private final LikeClient likeClient;
    private final ViewClient viewClient;

    private final ArticleQueryModelRepository repository;

    private final List<EventHandler> eventHandlers;


    /**
     * Consumer에 의해서 호출이 되고 Event에 대한 처리가 가능할 경우 처리를 한다.
     *
     * @param event
     */
    public void handleEvent(Event<EventPayload> event) {
        for (EventHandler eventHandler : eventHandlers) {
            if (eventHandler.isSupported(event)) {
                eventHandler.handle(event);
            }
        }
    }

    /**
     * 게시글에 대한 읽어오는 함수
     *
     * @param articleId
     * @return
     */
    public ArticleReadResponse readArticle(Long articleId) {
        ArticleQueryModel articleQueryModel = repository.readQueryModel(articleId)
                .or(() -> fetch(articleId))
                .orElseThrow();
        Long viewCount = viewClient.articleViewCount(articleId);
        return ArticleReadResponse.from(articleQueryModel, viewCount);
    }


    /**
     * Redis Repository에 값이 없을 떄 각 서비스에 요청을 해서 값을 가져오는 함수
     *
     * @param articleId
     * @return
     */
    Optional<ArticleQueryModel> fetch(Long articleId) {
        Optional<ArticleQueryModel> articleQueryModelOptional = articleClient.readArticle(articleId)
                .map(article -> ArticleQueryModel
                        .create(
                                article,
                                commentClient.getCommentCounts(articleId),
                                likeClient.articleLikeCount(articleId)
                        ));
        articleQueryModelOptional.ifPresent(articleQueryModel -> repository.saveQueryModel(articleQueryModel, Duration.ofDays(1)));

        log.info("[ArticleReadService.fetch] fetch data. articleId={}, isPresent={}", articleId, articleQueryModelOptional.isPresent());

        return articleQueryModelOptional;
    }
}
