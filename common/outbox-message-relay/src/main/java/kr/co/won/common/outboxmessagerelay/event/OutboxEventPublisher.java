package kr.co.won.common.outboxmessagerelay.event;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import kr.co.won.common.event.EventType;
import kr.co.won.common.outboxmessagerelay.constants.MessageRelayConstants;
import kr.co.won.common.outboxmessagerelay.entity.OutboxEntity;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final Snowflake outboxIdGenerator = new Snowflake();
    private final Snowflake eventIdGenerator = new Snowflake();

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 메세지 릴레이에 대해서 처리를 할 수 있도록 어플리케이션 내에서 이벤트를 발행을 하는 함수
     *
     * @param eventType
     * @param eventPayload
     * @param shardKey
     */
    public void publish(EventType eventType, EventPayload eventPayload, Long shardKey) {
        OutboxEntity outbox = OutboxEntity.create(
                outboxIdGenerator.nextId(),
                eventType,
                Event.of(eventIdGenerator.nextId(), eventType, eventPayload).toJson(),
                shardKey % MessageRelayConstants.SHARD_COUNT
        );
        // application 내에 이벤트 생성해서 발행을 한다.
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
    }
}
