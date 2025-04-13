package kr.co.won.articleread.cache;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class OptimizedCacheTTLTest {

    @DisplayName(value = "01. 논리적 TTL 과 물리적 TTL 설정 객체 테스트")
    @Test
    void createOptimizedCacheTTLTests() {
        long ttlSeconds = 10;

        OptimizedCacheTTL optimizedCacheTTL = OptimizedCacheTTL.of(ttlSeconds);

        Assertions.assertThat(optimizedCacheTTL.getLogicalTTL()).isEqualTo(Duration.ofSeconds(ttlSeconds));
        Assertions.assertThat(optimizedCacheTTL.getPhysicalTTL().getSeconds())
                .isEqualTo(Duration.ofSeconds(ttlSeconds)
                        .plusSeconds(OptimizedCacheTTL.PHYSICAL_TTL_DELAY_SECONDS)
                        .toSeconds());

    }

}