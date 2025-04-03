package kr.co.won.common.outboxmessagerelay.coordinator;

import jakarta.annotation.PreDestroy;
import kr.co.won.common.outboxmessagerelay.configuration.AssignedShard;
import kr.co.won.common.outboxmessagerelay.constants.MessageRelayConstants;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MessageRelayCoordinator {

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    // sorted set에 대한 키
    private final String APP_ID = UUID.randomUUID().toString();

    // ping에 대한 시간 정의
    private final int PING_INTERVAL_SECONDS = 3;

    // 최대 실패 횟수 정의
    private final int PING_FAILURE_THRESHOLD = 3;

    /**
     * {@link AssignedShard}를 만들어주는 함수
     *
     * @return
     */
    public AssignedShard assignShard() {
        return AssignedShard.of(APP_ID, findAppIds(), MessageRelayConstants.SHARD_COUNT);
    }

    /**
     * Redis Sorted Set에 정의가 되어 있는 목록 가져오기
     *
     * @return
     */
    private List<String> findAppIds() {
        return redisTemplate.opsForZSet().reverseRange(generateKey(), 0, -1)
                .stream()
                .sorted()
                .toList();
    }

    /**
     * 주기적으로 실행을 하는 스케줄 함수
     */
    @Scheduled(fixedDelay = PING_INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void ping() {
        // 한번의 통신으로 여러 개의 연산을 한꺼번에 전달을 할 수 있다.
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection connection = (StringRedisConnection) action;
            String key = generateKey();
            connection.zAdd(key, Instant.now().toEpochMilli(), APP_ID);
            connection.zRemRangeByScore(
                    key,
                    Double.NEGATIVE_INFINITY,
                    Instant.now().minusSeconds(PING_INTERVAL_SECONDS * PING_FAILURE_THRESHOLD).toEpochMilli()
            );
            return null;
        });

    }

    /**
     * Application 종료 시 자신의 APP_ID에 대한 정보를 삭제
     */
    @PreDestroy
    public void leave() {
        redisTemplate.opsForZSet()
                .remove(generateKey(), APP_ID);
    }


    /**
     * redis에서 사용할 sorted set에 대한 키를 생성을 해주는 함수
     *
     * @return
     */
    private String generateKey() {
        return "message-relay-coordinator::app-list::%s".formatted(applicationName);
    }
}
