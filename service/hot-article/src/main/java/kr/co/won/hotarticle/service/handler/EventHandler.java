package kr.co.won.hotarticle.service.handler;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;

public interface EventHandler<T extends EventPayload> {

    /**
     * Event 객체에 대해서 처리를 하는 함수
     *
     * @param event
     */
    void handle(Event<T> event);

    /**
     * 해당 Event를 지원을 하는지 알려주는 함수
     *
     * @param event
     * @return
     */
    boolean isSupport(Event<T> event);

    /**
     * 해당 이벤트에 해당이 되는 ArticleId에 대해서 찾아주는 함수
     *
     * @param event
     * @return
     */
    Long findArticleId(Event<T> event);
}
