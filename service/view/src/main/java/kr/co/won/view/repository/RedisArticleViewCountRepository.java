package kr.co.won.view.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisArticleViewCountRepository {

    /**
     * redis에 대한 의존성을 주입을 하면 기본적으로 String Template이 생성이 되어서 사용을 할 수 있다.
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * redis에서 사용할 Key에 대한 Format을 지정한 값이다.
     * => key의 형태는 view::article::{article_id}::view_count로 키를 정의를 한다.
     */
    private static final String KEY_FORMAT = "view::article::%s::view_count";

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }

    /**
     * 조회수를 가져오는 함수
     *
     * @param articleId
     * @return
     */
    public Long getViewCountByArticleId(Long articleId) {
        String viewCount = redisTemplate.opsForValue()
                .get(generateKey(articleId));
        return viewCount == null ? 0L : Long.valueOf(viewCount);
    }

    /**
     * 조회수를 1 증가를 시켜주는 함수
     *
     * @param articleId
     */
    public Long increaseViewCountByArticleId(Long articleId) {
        return redisTemplate.opsForValue().increment(generateKey(articleId), 1);
    }
}
