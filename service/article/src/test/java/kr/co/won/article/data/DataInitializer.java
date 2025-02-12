package kr.co.won.article.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.won.article.entity.ArticleEntity;
import kr.co.won.common.snowflake.Snowflake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class DataInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    Snowflake snowflake = new Snowflake();

    /**
     * Thread가 작업이 완료가 될 때까지 대기하기 위한 CountDownLatch 생성
     */
    CountDownLatch countDownLatch = new CountDownLatch(EXECUTE_COUNT);

    static final int BULK_INSERT_SIZE = 2000;

    static final int EXECUTE_COUNT = 6000;

    @Test
    void initializeTests() throws InterruptedException {
        /** 10개의 Thread 생성 */
        ExecutorService executorThread = Executors.newFixedThreadPool(10);
        /** Multi Thread 로 동작 */
        for (int i = 0; i < EXECUTE_COUNT; i++) {
            executorThread.submit(() -> {
                insert();
                countDownLatch.countDown();
                System.out.println("countDownLatch.getCount() = " + countDownLatch.getCount());
            });
        }
        countDownLatch.await();
        executorThread.shutdown();
    }

    void insert() {
        transactionTemplate.executeWithoutResult(status -> {
            for (int i = 0; i < BULK_INSERT_SIZE; i++) {
                ArticleEntity article = ArticleEntity.createArticle(snowflake.nextId(), "title" + i, "content " + i, 1L, 1L);
                entityManager.persist(article);
            }
        });
    }
}
