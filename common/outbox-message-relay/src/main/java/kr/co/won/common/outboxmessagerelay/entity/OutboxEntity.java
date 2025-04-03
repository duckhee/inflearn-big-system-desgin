package kr.co.won.common.outboxmessagerelay.entity;

import jakarta.persistence.*;
import kr.co.won.common.event.EventType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
@Entity
@Table(name = "tbl_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEntity {

    @Id
    private Long outboxId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String payload;

    private Long shardKey;

    private LocalDateTime createdAt;


    public static OutboxEntity create(Long outboxId, EventType eventType, String payload, Long shardKey) {
        OutboxEntity outboxEntity = new OutboxEntity();
        outboxEntity.outboxId = outboxId;
        outboxEntity.eventType = eventType;
        outboxEntity.payload = payload;
        outboxEntity.shardKey = shardKey;
        outboxEntity.createdAt = LocalDateTime.now();
        return outboxEntity;
    }

}
