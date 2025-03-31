package kr.co.won.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleViewCountRepository {

    private final StringRedisTemplate redisTemplate;

    // 조회 수에 대한 키 형태 정의 -> hot-article::article::{article-id}::view-count
    private static final String KEY_FORMAT = "hot-article::article::%s::view-count";

    /**
     * 조회수에 대해서 저장을 하기 위한 함수
     *
     * @param articleId
     * @param viewCount
     * @param ttl
     */
    public void createOrUpdate(Long articleId, Long viewCount, Duration ttl) {
        redisTemplate.opsForValue()
                .set(generateKey(articleId), String.valueOf(viewCount), ttl);
    }

    /**
     * 조회수에 대한 정보를 가져오기 위한 함수
     *
     * @param articleId
     * @return
     */
    public Long read(Long articleId) {
        String result = redisTemplate.opsForValue()
                .get(generateKey(articleId));
        if (result == null) {
            return 0l;
        }
        return Long.valueOf(result);
    }

    /**
     * 조회수에 대한 키를 만들어주는 함수
     *
     * @param articleId
     * @return
     */
    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
