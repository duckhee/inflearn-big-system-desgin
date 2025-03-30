package kr.co.won.hotarticle.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HotArticleListRepositoryTest {

    @Autowired
    private HotArticleListRepository repository;

    @DisplayName(value = "01. 인기글에 대한 추가를 하는 테스트")
    @Test
    void hotArticleAddTests() throws InterruptedException {
        LocalDateTime time = LocalDateTime.of(2024, 7, 23, 0, 0);
        long limit = 3; // 3건의 인기글만 가지고 있도록 설정

        // when
        repository.add(1l, time, 2l, limit, Duration.ofSeconds(3));
        repository.add(2l, time, 3l, limit, Duration.ofSeconds(3));
        repository.add(3l, time, 1l, limit, Duration.ofSeconds(3));
        repository.add(4l, time, 5l, limit, Duration.ofSeconds(3));
        repository.add(5l, time, 4l, limit, Duration.ofSeconds(3));

        // then
        List<Long> hotArticles = repository.readAll("20240723");


        Assertions.assertThat(hotArticles).hasSize(Long.valueOf(limit).intValue());
        assertEquals(hotArticles.get(0), 4l);
        assertEquals(hotArticles.get(1), 5l);
        assertEquals(hotArticles.get(2), 2l);

        TimeUnit.SECONDS.sleep(5);

        Assertions.assertThat(repository.readAll("20240723")).isEmpty();

    }

}