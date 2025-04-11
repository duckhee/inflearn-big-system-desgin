package kr.co.won.articleread.consumer;

import kr.co.won.articleread.service.ArticleReadService;
import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import kr.co.won.common.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleReadEventConsumer {

    private final ArticleReadService articleReadService;

    @KafkaListener(topics = {
            EventType.Topic.KUKE_BOARD_ARTICLE,
            EventType.Topic.KUKE_BOARD_COMMENT,
            EventType.Topic.KUKE_BOARD_LIKE
    })
    public void listenEvent(String message, Acknowledgment acknowledgment) {
        log.info("[ArticleReadEventConsumer.listenEvent] message={}", message);
        Event<EventPayload> eventPayloadEvent = Event.fromJson(message);
        if (eventPayloadEvent != null) {
            articleReadService.handleEvent(eventPayloadEvent);
        }
        acknowledgment.acknowledge();
    }
}
