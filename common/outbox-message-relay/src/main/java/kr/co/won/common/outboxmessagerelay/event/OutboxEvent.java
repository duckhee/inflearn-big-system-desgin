package kr.co.won.common.outboxmessagerelay.event;

import kr.co.won.common.outboxmessagerelay.entity.OutboxEntity;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class OutboxEvent {

    private OutboxEntity outbox;

    public static OutboxEvent of(OutboxEntity outboxEntity) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.outbox = outboxEntity;
        return outboxEvent;
    }
}
