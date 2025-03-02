package kr.co.won.comment.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.won.comment.entity.CommentEntity;
import kr.co.won.comment.entity.CommentPath;
import kr.co.won.comment.entity.InfinityCommentEntity;
import kr.co.won.common.snowflake.Snowflake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class InfinityCommentDataInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;


    /**
     * path에서 사용을 할 문자열에 대한 정의
     */
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 하나의 Depth 당 사용할 문자열의 갯수
     */
    private static final int DEPTH_CHUNK_SIZE = 5;

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
            int start = i * BULK_INSERT_SIZE;
            int end = (i + 1) * BULK_INSERT_SIZE;
            executorThread.submit(() -> {
                insert(start, end);
                countDownLatch.countDown();
                System.out.println("countDownLatch.getCount() = " + countDownLatch.getCount());
            });
        }
        countDownLatch.await();
        executorThread.shutdown();
    }

    void insert(int start, int end) {
        transactionTemplate.executeWithoutResult(status -> {
            InfinityCommentEntity previousComment = null;
            for (int i = start; i < end; i++) {
                InfinityCommentEntity comment = InfinityCommentEntity.create(snowflake.nextId(), "title" + i, 1l, 1L, toPath(i));
                previousComment = comment;
                entityManager.persist(comment);
            }
        });
    }

    CommentPath toPath(int value) {

        String resultPath = "";
        /** path를 구하는 과정 */
        for (int i = 0; i < DEPTH_CHUNK_SIZE; i++) {
            resultPath = CHARSET.charAt(value % CHARSET.length()) + resultPath;
            value /= CHARSET.length();
        }
        return CommentPath.create(resultPath);
    }

}
