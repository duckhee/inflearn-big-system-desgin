package kr.co.won.articleread.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizedCacheLockProvider {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "optimized-cache-lock::";
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);

    /**
     * 동시성에 대한 문제를 해결을 하기 위해서 사용을 하는 Lock에 대해서 얻는 함수
     *
     * @param key
     * @return
     */
    public boolean getLock(String key) {
        return redisTemplate.opsForValue().setIfAbsent(generateLockKey(key), "", LOCK_TTL);
    }

    /**
     * 동시성에 대한 처리를 할 때 획득을 하는 Lock에 대해서 해제를 해주는 함수
     *
     * @param key
     */
    public void unLock(String key) {
        redisTemplate.delete(generateLockKey(key));
    }

    /**
     * 동시성에 대한 처리를 하기 위해서 Lock에 대한 키 값을 구현을 하는 함수
     *
     * @param key
     * @return
     */
    private String generateLockKey(String key) {
        return KEY_PREFIX + key;
    }
}
