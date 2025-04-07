package kr.co.won.hotarticle.consumer;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import kr.co.won.common.event.EventType;
import kr.co.won.hotarticle.service.HotArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotArticleConsumer {

    private final HotArticleService hotArticleService;

    /**
     * Kafka 에서 소비할 Topic에 대해서 정의
     * => 해당 토픽으로 전달 받은 이벤트에 대해서 처리를 한다.
     *
     * @param message        전달 받은 데이터에 대한 문자열
     * @param acknowledgment {@link Acknowledgment}는 해당 이벤트를 전달을 받아서 치러를 했다고 Kafka에 알려줄 때 사용이 된다.
     */
    @KafkaListener(topics = {
            EventType.Topic.KUKE_BOARD_ARTICLE,
            EventType.Topic.KUKE_BOARD_COMMENT,
            EventType.Topic.KUKE_BOARD_LIKE,
            EventType.Topic.KUKE_BOARD_VIEW
    })
    public void consumerEventHandler(String message, Acknowledgment acknowledgment) {

        log.info("[HotArticleConsumer.consumerEventHandler] received msg = {}", message);
        Event<EventPayload> event = Event.fromJson(message);
        log.info("[HotArticleConsumer.consumerEventHandler]event = {}", event.getType().name());
        if (event != null) {
            log.info("[HotArticleConsumer.consumerEventHandler] handleEvent");
            hotArticleService.handleEvent(event);
        }
        // kafka에 해당 구독에 대해서 처리가 끝났다고 알려주는 함수 호출
        acknowledgment.acknowledge();
    }

}
