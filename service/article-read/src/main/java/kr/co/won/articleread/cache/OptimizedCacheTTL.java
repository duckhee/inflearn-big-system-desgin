package kr.co.won.articleread.cache;

import lombok.Getter;

import java.time.Duration;

@Getter
public class OptimizedCacheTTL {

    /// logical ttl과 physical ttl 시간 차이
    public static final long PHYSICAL_TTL_DELAY_SECONDS = 5;

    private Duration logicalTTL;

    private Duration physicalTTL;

    ///  전달 받는 값은 논리적 TTL 시간이다.
    public static OptimizedCacheTTL of(long ttlSeconds) {
        OptimizedCacheTTL optimizedCacheTTL = new OptimizedCacheTTL();
        optimizedCacheTTL.logicalTTL = Duration.ofSeconds(ttlSeconds);
        optimizedCacheTTL.physicalTTL = Duration.ofSeconds(ttlSeconds).plus(Duration.ofSeconds(PHYSICAL_TTL_DELAY_SECONDS));
        return optimizedCacheTTL;

    }

}
