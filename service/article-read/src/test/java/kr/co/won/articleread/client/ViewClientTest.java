package kr.co.won.articleread.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

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
}