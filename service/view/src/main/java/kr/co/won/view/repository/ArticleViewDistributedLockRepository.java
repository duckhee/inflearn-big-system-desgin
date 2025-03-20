package kr.co.won.view.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class ArticleViewDistributedLockRepository {

    /**
     * Abusing 정책을 적용하기 위한 redis template
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 사용할 Key
     * view:article:{article_id}:user:{user_id}:lock
     */
    private static final String KEY_FORMAT = "view::article::%s::user::%s::lock";

    private String generateKey(Long articleId, Long userId) {
        return KEY_FORMAT.formatted(articleId, userId);
    }

    /**
     * Lock에 대한 획득 하는 함수
     *
     * @param articleId
     * @param userId
     * @param ttlTime   -> 락을 걸어줄 시간
     * @return
     */
    public boolean getLock(Long articleId, Long userId, Duration ttlTime) {
        String lockKey = generateKey(articleId, userId);
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "", ttlTime);
    }


}
