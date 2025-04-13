package kr.co.won.articleread.client;

import ch.qos.logback.core.util.TimeUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.TimeoutUtils;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ViewClientTest {

    @Autowired
    private ViewClient viewClient;

    @DisplayName(value = "01. view count에 대한 cache 에 대한 동작 확인을 위한 테스트")
    @Test
    void readCacheableTests() throws InterruptedException {
        viewClient.articleViewCount(1l);
        viewClient.articleViewCount(1l);
        viewClient.articleViewCount(1l);

        TimeUnit.SECONDS.sleep(3);

        viewClient.articleViewCount(1l);
    }

    @DisplayName(value = "02. view count 에 대한 multi thread에 대한 요청으로 캐시 처리 트래픽 확인 테스트")
    @Test
    void multiThreadViewCountTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        ///  init cache
        viewClient.articleViewCount(1l);

        for (int i = 0; i < 5; i++) {
            CountDownLatch countDownLatch = new CountDownLatch(5);
            for (int j = 0; j < 5; j++) {
                executorService.execute(() -> {
                    viewClient.articleViewCount(1l);
                    countDownLatch.countDown();
                });
            }
            countDownLatch.await();
            TimeUnit.SECONDS.sleep(2);
            System.out.println("========== cache expired ==========");

        }
    }

}