package kr.co.won.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleLikeCountRepository {

    private final StringRedisTemplate redisTemplate;

    // 좋아요 수를 저장을 할 키 타입 정의 hot-article::article::{articleId}::like-count
    private static final String KEY_FORMAT = "hot-article::article::%s::like-count";

    public void createOrUpdate(Long articleId, Long likeCount, Duration ttl) {
        redisTemplate.opsForValue()
                .set(generateKey(articleId), String.valueOf(likeCount), ttl);
    }

    public Long read(Long articleId) {
        String result = redisTemplate.opsForValue()
                .get(generateKey(articleId));
        if (result == null) {
            return 0l;
        }
        return Long.valueOf(result);
    }

    /**
     * 좋아요 수를 저장을 할 키에 대해서 생성을 하는 함수
     *
     * @param articleId
     * @return
     */
    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
