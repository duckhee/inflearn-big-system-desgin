package kr.co.won.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 오늘 생성된 게시글인지 확인이 필요하기 때문에 게시글의 생성 시간이 필요하다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleCreatedTimeRepository {

    private final StringRedisTemplate redisTemplate;

    // 생성 시간에 대해서 redis 에서 사용을 하는 키 정의 -> hot-article::article::{articleId}::created-time
    private final static String KEY_FORMAT = "hot-article::article::%s::created-time";

    /**
     * 인기글이 생성된 시간을 저장 및 수정 하는 함수
     *
     * @param articleId
     * @param createdTime
     * @param ttl
     */
    public void createOrUpdate(Long articleId, LocalDateTime createdTime, Duration ttl) {
        redisTemplate.opsForValue()
                .set(generateKey(articleId), String.valueOf(createdTime.toInstant(ZoneOffset.UTC).toEpochMilli()), ttl); // UTC 시간으로 변환을 해서 저장
    }

    /**
     * 인기글에 대해서 생성된 시간을 제거를 하는 함수
     *
     * @param articleId
     */
    public void delete(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }

    /**
     * 인기글이 생성된 시간을 가져오는 함수
     *
     * @param articleId
     * @return
     */
    public LocalDateTime read(Long articleId) {
        String result = redisTemplate.opsForValue()
                .get(generateKey(articleId));
        if (result == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(Long.valueOf(result)), ZoneOffset.UTC
        );
    }

    private String generateKey(Long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }
}
