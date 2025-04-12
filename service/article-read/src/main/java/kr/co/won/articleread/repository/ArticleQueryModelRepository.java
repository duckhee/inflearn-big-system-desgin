package kr.co.won.articleread.repository;

import kr.co.won.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ArticleQueryModelRepository {
    private final StringRedisTemplate redisTemplate;

    // article-read::article::{articleId}
    private final static String KEY_FORMAT = "article-read::article::%s";

    public void saveQueryModel(ArticleQueryModel queryModel, Duration ttl) {
        redisTemplate.opsForValue()
                .set(generateKey(queryModel), DataSerializer.serialized(queryModel), ttl);
    }

    public void updateQueryModel(ArticleQueryModel queryModel) {
        redisTemplate.opsForValue().setIfPresent(generateKey(queryModel), DataSerializer.serialized(queryModel));
    }

    public void deleteQueryModel(Long articleId) {
        redisTemplate.delete(generateKey(articleId));
    }

    public Optional<ArticleQueryModel> readQueryModel(Long articleId) {
        String readStringQueryModel = redisTemplate.opsForValue()
                .get(generateKey(articleId));
        return Optional.ofNullable(readStringQueryModel)
                .map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class));
    }

    private String generateKey(ArticleQueryModel articleQueryModel) {
        return generateKey(articleQueryModel.getArticleId());
    }

    private String generateKey(long articleId) {
        return KEY_FORMAT.formatted(articleId);
    }

    public Map<Long, ArticleQueryModel> readAllQueryModel(List<Long> articleIds) {
        List<String> articleKeys = articleIds.stream().map(this::generateKey).toList();
        return redisTemplate.opsForValue().multiGet(articleKeys)
                .stream().filter(Objects::nonNull)
                .map(json -> DataSerializer.deserialize(json, ArticleQueryModel.class))
                .collect(Collectors.toMap(ArticleQueryModel::getArticleId, Function.identity()));
    }
}
