package kr.co.won.hotarticle.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HotArticleListRepository {

    private final StringRedisTemplate redisTemplate;

    // ZSET 을 이용한 데이터 저장
    // 데이터에 대한 키는 hot-article::list::{yyyyMMdd}
    private static final String KEY_FORMAT = "hot-article::list::%s";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 인기글에 대한 추가를 해주는 함수
     *
     * @param articleId
     * @param time
     * @param score
     * @param limit
     * @param ttl
     */
    public void add(Long articleId, LocalDateTime time, Long score, Long limit, Duration ttl) {

        // pipeline을 만들어서 제어를 하기 위한 executePipelined를 이용을 한다.
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection connection = (StringRedisConnection) action;
            String key = generateKey(time);
            connection.zAdd(key, score, String.valueOf(articleId));
            connection.zRemRange(key, 0, -limit - 1); // 10개의 데이터만 남겨주기 위한 Remove Range 를 이용한다.
            connection.expire(key, ttl.getSeconds()); // 만료 시간에 대해서 설정을 한다.
            return null;
        });
    }

    /**
     * 인기글에 대한 목록을 전부 가져오는 함수
     *
     * @param key
     * @return
     */
    public List<Long> readAll(String key) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(generateKey(key), 0, -1)  // 해당 키에 해당이 되는 정보를 가져오기 위한 설정 -> 범위를 가지고 값을 가져온다.
                .stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .toList();
    }

    /**
     * 인기글에 대해서 삭제를 하는 함수
     *
     * @param articleId
     * @param keyDateTime
     */
    public void remove(Long articleId, LocalDateTime keyDateTime) {
        redisTemplate.opsForZSet()
                .remove(generateKey(keyDateTime), String.valueOf(articleId));
    }

    /**
     * 키에 들어가는 시간 문자열을 이용해서 키를 생성을 해주는 함수
     *
     * @param time
     * @return
     */
    private String generateKey(LocalDateTime time) {
        String timeFormat = TIME_FORMATTER.format(time);
        return generateKey(timeFormat);
    }

    /**
     * redis에 대한 인기글을 위한 key 생성 함수
     *
     * @param dateStr
     * @return
     */
    private String generateKey(String dateStr) {
        return KEY_FORMAT.formatted(dateStr);
    }
}
