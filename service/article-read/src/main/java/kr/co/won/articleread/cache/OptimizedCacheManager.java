package kr.co.won.articleread.cache;

import kr.co.won.dataserializer.DataSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OptimizedCacheManager {

    private final StringRedisTemplate redisTemplate;

    private final OptimizedCacheLockProvider optimizedCacheLockProvider;

    private static final String DELIMITER = "::";

    /**
     * 최적화된 캐시에서 데이터를 가져오는 함수
     * => 논리적인 만료가 되면, 해당 값을 다시 가져와서 캐시를 만들어준다.
     *
     * @param type
     * @param ttlSeconds
     * @param arguments
     * @param returnClassType
     * @param originDataSupplier
     * @return
     * @throws Throwable
     */
    public Object process(
            String type,
            long ttlSeconds,
            Object[] arguments,
            Class<?> returnClassType,
            OptimizedCacheOriginDataSupplier<?> originDataSupplier
    ) throws Throwable {
        String key = generateCacheKey(type, arguments);
        String cacheData = redisTemplate.opsForValue().get(key);
        ///  캐시가 없는 경우
        if (cacheData == null) {
            return refreshCache(originDataSupplier, key, ttlSeconds);
        }
        ///  최적화된 캐시의 객체로 변환
        OptimizedCache deserializeCache = DataSerializer.deserialize(cacheData, OptimizedCache.class);
        ///  역직렬화 실패 시
        if (deserializeCache == null) {
            return refreshCache(originDataSupplier, key, ttlSeconds);
        }
        ///  만료가 되지 않은 경우
        if (!deserializeCache.isExpired()) {
            return deserializeCache.parseData(returnClassType);
        }
        ///  lock을 획득하지 못한 경우
        if (!optimizedCacheLockProvider.getLock(key)) {
            return deserializeCache.parseData(returnClassType);
        }
        /// lock에 대한 획득한 경우 -> 1개의 요청만 들어오게 된다.
        try {
            return refreshCache(originDataSupplier, key, ttlSeconds);
        } finally {
            optimizedCacheLockProvider.unLock(key);
        }
    }

    /**
     * 다시 논리적 캐시에 대한 만료 시간 도래를 했기 때문에 데이터를 가져와서 다시 캐시를 생성을 하는 함수
     *
     * @param originDataSupplier {@link OptimizedCacheOriginDataSupplier}
     * @param key
     * @param ttlSeconds
     * @return
     */
    private Object refreshCache(OptimizedCacheOriginDataSupplier<?> originDataSupplier, String key, long ttlSeconds) throws Throwable {
        ///  원본 데이터에 대해서 가져오기
        Object result = originDataSupplier.get();
        OptimizedCacheTTL optimizedCacheTTL = OptimizedCacheTTL.of(ttlSeconds);
        OptimizedCache optimizedCache = OptimizedCache.of(result, optimizedCacheTTL.getLogicalTTL());
        redisTemplate.opsForValue().set(key, DataSerializer.serialized(optimizedCache), optimizedCacheTTL.getPhysicalTTL());
        return result;
    }

    /**
     * 최적화된 캐시를 생성할 떄 사용할 키에 대한 만들어 주는 함수
     *
     * @param prefix
     * @param arguments
     * @return
     */
    private String generateCacheKey(String prefix, Object[] arguments) {
        return prefix + DELIMITER + Arrays.stream(arguments)
                .map(String::valueOf)
                .collect(Collectors.joining(DELIMITER));
    }
}
