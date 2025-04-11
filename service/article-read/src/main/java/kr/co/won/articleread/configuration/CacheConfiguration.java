package kr.co.won.articleread.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Map;

@EnableCaching // spring에서 제공을 하는 cache를 사용을 하기 위해서 사용할 수 있도록 설정하는 어노테이션
@Configuration
public class CacheConfiguration {

    /**
     * Cache에 대한 RedisCache로 설정
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(
                        Map.of(
                                "articleViewCount", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(1)) // cache에 대한 키 값은 articleViewCount로 지정을 하고 만료 시간은 1초
                        )
                )
                .build();
    }
}
