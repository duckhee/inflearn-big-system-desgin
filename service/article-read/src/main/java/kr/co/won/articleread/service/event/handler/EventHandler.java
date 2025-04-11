package kr.co.won.articleread.service.event.handler;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;

public interface EventHandler<T extends EventPayload> {

    /**
     * 이벤트에 대해서 처리를 하는 함수
     *
     * @param eventPayload
     */
    void handle(Event<T> eventPayload);

    /**
     * 해당 이벤트 핸들러가 이벤트를 처리를 할 수 있는지 알려주는 함수
     *
     * @param eventPayload
     * @return
     */
    boolean isSupported(Event<T> eventPayload);
}
