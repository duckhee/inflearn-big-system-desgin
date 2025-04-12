package kr.co.won.articleread.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArticleIdListRepository {

    private final StringRedisTemplate redisTemplate;

    // key에 대한 값 지정 -> article-read::board::{boardId}::article-list
    private final static String ARTICLE_ID_LIST_KEY = "article-read::board::%s::article-list";


    /**
     * articleId에 대한 목록을 1000개만 유지하도록 추가하는 함수
     *
     * @param boardId
     * @param articleId
     * @param limit
     */
    public void addArticleIdList(Long boardId, Long articleId, Long limit) {
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection connection = (StringRedisConnection) action;
            String key = generateKey(boardId);

            // sorted set -> score는 double 값이기 때문에 큰 수의 long 값이 들어오면 목록이 꼬일 수 있기 때문에 0의 score를 하고 문자열의 값에 따라 정렬이 되는 것을 이용을 한다.
            connection.zAdd(key, 0, toPaddedString(articleId));
            connection.zRange(key, 0, -limit - 1);
            return null;
        });
    }

    /**
     * 게시판 아이디 리스트에서 해당 값을 제거해주는 함수
     *
     * @param boardId
     * @param articleId
     */
    public void deleteArticleId(Long boardId, Long articleId) {
        redisTemplate.opsForZSet().remove(generateKey(boardId), toPaddedString(articleId));
    }

    /**
     * 페이징 방식에서 사용할 게시판 아이디 리스트 반환 함수
     *
     * @param boardId
     * @param offset
     * @param limit
     * @return
     */
    public List<Long> getArticleIdList(Long boardId, Long offset, Long limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(generateKey(boardId), offset, offset + limit - 1)
                .stream().map(Long::valueOf).collect(Collectors.toList());
    }

    /**
     * 무한 스크롤 방식에서 사용할 게시판 아이디 리스트 반환 함수
     *
     * @param boardId
     * @param lastArticleId
     * @param limit
     * @return
     */
    public List<Long> getArticleIdListInfinityScroll(Long boardId, Long lastArticleId, Long limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeByLex(
                        generateKey(boardId),
                        lastArticleId == null ? Range.unbounded() : Range.leftUnbounded(Range.Bound.exclusive(toPaddedString(lastArticleId))), // lastArticleId이 없으면 limit 만큼 lastArticleId이 있으면, 해당 값의 오른쪽 부터 시작
                        Limit.limit().count(limit.intValue())
                ).stream().map(Long::valueOf).collect(Collectors.toList());
    }


    /**
     * 입력 받은 숫자를 고정된 자리수의 문자열로 변환을 해주는 함수
     *
     * @param articleId
     * @return
     */
    private String toPaddedString(Long articleId) {
        return "%019d".formatted(articleId);
    }

    private String generateKey(Long boardId) {
        return ARTICLE_ID_LIST_KEY.formatted(boardId);
    }
}
