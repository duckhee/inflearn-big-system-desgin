package kr.co.won.common.outboxmessagerelay;

import kr.co.won.common.outboxmessagerelay.configuration.AssignedShard;
import kr.co.won.common.outboxmessagerelay.coordinator.MessageRelayCoordinator;
import kr.co.won.common.outboxmessagerelay.entity.OutboxEntity;
import kr.co.won.common.outboxmessagerelay.event.OutboxEvent;
import kr.co.won.common.outboxmessagerelay.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {

    private final OutboxRepository outboxRepository;
    private final MessageRelayCoordinator messageRelayCoordinator;
    private final KafkaTemplate<String, String> messageRelayKafkaTemplate;

    /**
     * Transaction에 대한 Commit이 일어나기 전에 실행이 되는 함수
     *
     * @param outboxEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent={}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }


    /**
     * Transaction에 대한 Commit이 된 후 비동기로 카프카에 메세지를 발행을 한다.
     *
     * @param outboxEvent
     */
    @Async("messageRelayPublishEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutboxEvent outboxEvent) {
        eventPublish(outboxEvent.getOutbox());
    }


    private void eventPublish(OutboxEntity outboxEntity) {
        try {
            // 비동기로 처리를 하기 때문에 future를 반환을 한다.
            CompletableFuture<SendResult<String, String>> future = messageRelayKafkaTemplate.send(
                    outboxEntity.getEventType().getTopic(),
                    String.valueOf(outboxEntity.getShardKey()), // 해당 값은 파티션에 대한 값이 들어가는데 동일한 샤드 키를 이용을 할 경우 동일한 샤드키로 들어간다.
                    outboxEntity.getPayload()
            );
            // 해당 전송에 대해서 1초간 대기를 하면서 값을 받아온다.
            future.get(1, TimeUnit.SECONDS);
            outboxRepository.delete(outboxEntity);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
//        }
        } catch (Exception e) {
            log.error("[MessageRelay.publishEvent] outbox={}", outboxEntity, e);
        }
    }


    /**
     * 미전송이 된 이벤트에 대한 발행을 하는 스케쥴러
     */
    @Scheduled(
            fixedDelay = 10,
            initialDelay = 5,
            timeUnit = TimeUnit.SECONDS,
            scheduler = "messageRelayPublishPendingEventExecutor"
    )
    public void publishPendingEvent() {
        AssignedShard assignedShard = messageRelayCoordinator.assignShard();
        log.info("[MessageRelay.publishPendingEvent] assignedShard size={}", assignedShard.getShards().size());
        for (Long shard : assignedShard.getShards()) {
            List<OutboxEntity> outboxes = outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    shard,
                    LocalDateTime.now().minusSeconds(10),
                    Pageable.ofSize(100)
            );
            for (OutboxEntity outbox : outboxes) {
                eventPublish(outbox);
            }
        }
    }
}
