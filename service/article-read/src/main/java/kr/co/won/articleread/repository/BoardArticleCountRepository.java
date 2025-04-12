package kr.co.won.articleread.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BoardArticleCountRepository {

    private final StringRedisTemplate redisTemplate;

    // 키의 형태 ->  article-read::board-article-count::board::{boardId}
    private final static String ARTICLE_COUNT_KEY_FORMAT = "article-read::board-article-count::board::%s";

    /**
     * Board에 있는 Article의 갯수에 대해서 변화를 저장을 하는 함수
     *
     * @param boardId
     * @param articleCount
     */
    public void createOrUpdateArticleCount(Long boardId, Long articleCount) {
        redisTemplate.opsForValue().set(generateKey(boardId), String.valueOf(articleCount));
    }

    /**
     * Board에 있는 Article의 개수를 반환하는 함수
     *
     * @param boardId
     * @return
     */
    public Long getBoardArticleCount(Long boardId) {
        String result = redisTemplate.opsForValue().get(generateKey(boardId));
        if (result == null) {
            return 0l;
        }
        return Long.parseLong(result);
    }


    private String generateKey(Long boardId) {
        return ARTICLE_COUNT_KEY_FORMAT.formatted(boardId);
    }
}
