package kr.co.won.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleCommentCountRepository {

    private final StringRedisTemplate redisTemplate;

    // 사용을 할 키에 대한 정의 hot-article::article::{article-id}::comment-count
    private final static String KEY_FORMAT = "hot-article::article::%s::comment-count";

    /**
     * comment의 수에 대해서 생성 및 업데이트를 해주는 함수
     *
     * @param articleId
     * @param commentCount
     * @param ttl
     */
    public void createOrUpdate(Long articleId, Long commentCount, Duration ttl) {
        // 값이 있으면 수정이 되고 값이 없으면 생성을 하기 위해서 opsForValue를 이용을 한다.
        redisTemplate.opsForValue()
                .set(generateKey(articleId), String.valueOf(commentCount), ttl);
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
     * 키를 생성을 해주는 함수
     *
     * @param articleId
     * @return
     */
    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
