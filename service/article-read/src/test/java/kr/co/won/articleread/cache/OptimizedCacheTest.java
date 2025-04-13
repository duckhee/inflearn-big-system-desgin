package kr.co.won.articleread.cache;


import lombok.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class OptimizedCacheTest {

    @DisplayName(value = "01. optimizedCache에 대한 테스트")
    @Test
    public void parseDataTest() {
        createOptimizedCacheTests("data", 10);
        createOptimizedCacheTests(10l, 10);
        createOptimizedCacheTests(1, 10);
        TestClass hello = new TestClass("hello");
        createOptimizedCacheTests(hello, 10);

    }

    @DisplayName(value = "02. 만료 기간에 대한 테스트")
    @Test
    void isExpiredTests() {
        Assertions.assertThat(OptimizedCache.of("data", Duration.ofSeconds(-30)).isExpired()).isTrue();
        Assertions.assertThat(OptimizedCache.of("data", Duration.ofSeconds(30)).isExpired()).isFalse();
    }

    void createOptimizedCacheTests(Object data, long ttl) {
        // given
        OptimizedCache optimizedCache = OptimizedCache.of(data, Duration.ofSeconds(ttl));
        System.out.println("optimizedCache = " + optimizedCache);

        // when
        Object resolveData = optimizedCache.parseData(data.getClass());

        // then
        System.out.println("resolveData = " + resolveData);
        Assertions.assertThat(resolveData).isEqualTo(data);

    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestClass {
        String testData;
    }

}