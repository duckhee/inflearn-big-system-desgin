package kr.co.won.articleread.service;

import kr.co.won.articleread.client.ArticleClient;
import kr.co.won.articleread.client.CommentClient;
import kr.co.won.articleread.client.LikeClient;
import kr.co.won.articleread.client.ViewClient;
import kr.co.won.articleread.repository.ArticleIdListRepository;
import kr.co.won.articleread.repository.ArticleQueryModel;
import kr.co.won.articleread.repository.ArticleQueryModelRepository;
import kr.co.won.articleread.repository.BoardArticleCountRepository;
import kr.co.won.articleread.service.event.handler.EventHandler;
import kr.co.won.articleread.service.response.ArticleReadPageResponse;
import kr.co.won.articleread.service.response.ArticleReadResponse;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReadService {

    private final ArticleClient articleClient;
    private final CommentClient commentClient;
    private final LikeClient likeClient;
    private final ViewClient viewClient;

    private final ArticleQueryModelRepository repository;
    private final ArticleIdListRepository articleIdListRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

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

    public ArticleReadPageResponse readArticlePageResponse(Long boardId, Long page, Long pageSize) {
        List<Long> articleIds = readArticleIdLists(boardId, page, pageSize);
        List<ArticleReadResponse> articles = readArticleLists(articleIds);
        Long articleCount = readArticleCount(boardId);
        return ArticleReadPageResponse.of(articles, articleCount);
    }

    public List<ArticleReadResponse> readArticleInfinityScroll(Long boardId, Long lastArticleId, Long limit) {
        List<Long> infinityArticleIds = readArticleIdInfinityScroll(boardId, lastArticleId, limit);
        return readArticleLists(infinityArticleIds);
    }


    /**
     * 게시글에 대한 응답 목록을 만들어 주는 함수
     *
     * @param articleIds
     * @return
     */
    private List<ArticleReadResponse> readArticleLists(List<Long> articleIds) {
        Map<Long, ArticleQueryModel> articleQueryModelMap = repository.readAllQueryModel(articleIds);

        return articleIds.stream()
                .map(articleId -> articleQueryModelMap.containsKey(articleId) ? articleQueryModelMap.get(articleId) : fetch(articleId).orElse(null))
                .filter(Objects::nonNull)
                .map(articleQueryModel -> ArticleReadResponse.from(articleQueryModel, viewClient.articleViewCount(articleQueryModel.getArticleId())))
                .toList();
    }

    /**
     * 게시글에 대한 아이디 값에 대한 리스트 조회를 해서 반환을 하는 함수
     *
     * @param boardId
     * @param page
     * @param pageSize
     * @return
     */
    private List<Long> readArticleIdLists(Long boardId, Long page, Long pageSize) {
        List<Long> articleIdList = articleIdListRepository.getArticleIdList(boardId, (page - 1) * pageSize, pageSize);
        // redis에 모두 저장이 되어 있는 상태일 경우
        if (pageSize == articleIdList.size()) {
            log.info("[ArticleReadService.readArticlePageResponse] return redis data.");
            return articleIdList;
        }
        log.info("[ArticleReadService.readArticlePageResponse] return original data.");
        ArticleClient.ArticlePageResponse articlePageResponse = articleClient.pagingArticleListResponse(boardId, page, pageSize);
        return articlePageResponse.getArticles().stream().map(ArticleClient.ArticleResponse::getArticleId).collect(Collectors.toList());
    }

    /**
     * 현재 게시글에 대한 개수를 반환을 하는 함수
     *
     * @param boardId
     * @return
     */
    private Long readArticleCount(Long boardId) {
        Long boardArticleCount = boardArticleCountRepository.getBoardArticleCount(boardId);
        if (boardArticleCount != null) {
            return boardArticleCount;
        }
        long articleCount = articleClient.countArtice(boardId);
        boardArticleCountRepository.createOrUpdateArticleCount(boardId, articleCount);
        return articleCount;
    }

    private List<Long> readArticleIdInfinityScroll(Long boardId, Long lastArticleId, Long limit) {
        List<Long> articleIdListInfinityScroll = articleIdListRepository.getArticleIdListInfinityScroll(boardId, lastArticleId, limit);
        if (limit == articleIdListInfinityScroll.size()) {
            log.info("[ArticleReadService.readArticlePageResponse] return redis data.");
            return articleIdListInfinityScroll;
        }
        log.info("[ArticleReadService.readArticlePageResponse] return original data.");

        return articleClient.infinityScrollArticleListResponse(boardId, lastArticleId, limit)
                .stream().map(ArticleClient.ArticleResponse::getArticleId).collect(Collectors.toList());
    }
}
