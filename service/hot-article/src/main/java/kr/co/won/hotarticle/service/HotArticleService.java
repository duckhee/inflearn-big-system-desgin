package kr.co.won.hotarticle.service;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import kr.co.won.common.event.EventType;
import kr.co.won.hotarticle.client.ArticleClient;
import kr.co.won.hotarticle.repository.HotArticleListRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import kr.co.won.hotarticle.service.response.HotArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleService {

    // 원본 정보에 대해서 조회를 해서 같이 반환을 하기 위한 RestClient
    private final ArticleClient articleClient;

    // 이벤트에 대한 Handler 구현체들을 전부 주입을 받기 위해서 List 선언
    private final List<EventHandler> eventHandlers;

    private final HotArticleListRepository hotArticleListRepository;
    private final HotArticleScoreUpdater hotArticleScoreUpdater;

    /**
     * 이벤트를 카프카를 통해서 전달을 받는다.
     *
     * @param event
     */
    public void handleEvent(Event<EventPayload> event) {
        log.info("[HotArticleService.handleEvent] event={}", event.getType().name());
        EventHandler<EventPayload> eventHandler = findEventHandler(event);
        if (eventHandler == null) {
            log.info("[HotArticleService.handleEvent] not match event handler ={}", event.getType().name());
            return;
        }
        // 해당 이벤트가 생성 및 수정 이벤트인지 확인
        if (isArticleCreateOrUpdate(event)) {
            log.info("[HotArticleService.handleEvent] createOrUpdate = {}", event.getType().name());
            eventHandler.handle(event);
        } else {
            log.info("[HotArticleService.handleEvent] event = {}", event.getType().name());
            hotArticleScoreUpdater.update(event, eventHandler);
        }
    }

    /**
     * 인기글에 대한 정보를 가져오는 함수
     * => 게시글 서비스에서 인기글에 대한 정보를 가져와서 변환을 한 후 반환을 한다.
     *
     * @param dateStr
     * @return
     */
    public List<HotArticleResponse> readAll(String dateStr) {
        log.info("dateStr = {}", dateStr);
        List<HotArticleResponse> response = hotArticleListRepository.readAll(dateStr)
                .stream()
                .map(articleClient::read)
                .filter(Objects::nonNull)
                .map(HotArticleResponse::from)
                .toList();
        log.info("response = {}", response);
        return response;
    }


    /**
     * 처리를 할 수 있는 eventHandler를 찾아주는 함수
     * 처리를 할 수 있는 이벤트 핸들러가 없는 경우 null 반환
     *
     * @param event
     * @return
     */
    private EventHandler<EventPayload> findEventHandler(Event<EventPayload> event) {
        return eventHandlers.stream()
                .filter(eventHandler -> eventHandler.isSupport(event))
                .findAny()
                .orElse(null); // 찾지 못한 경우 null 반환
    }


    /**
     * 이벤트에 대한 타입을 확인을 하는 함수
     *
     * @param event
     * @return
     */
    private boolean isArticleCreateOrUpdate(Event<EventPayload> event) {
        return EventType.ARTICLE_CREATE == event.getType() || EventType.ARTICLE_DELETE == event.getType();
    }

}
