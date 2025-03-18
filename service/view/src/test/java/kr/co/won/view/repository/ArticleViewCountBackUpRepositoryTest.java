package kr.co.won.view.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.won.view.entity.ViewCountEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ArticleViewCountBackUpRepositoryTest {

    @Autowired
    ArticleViewCountBackUpRepository backUpRepository;

    @PersistenceContext
    EntityManager entityManager;

    @DisplayName(value = "01. 조회수에 대한 백업을 위한 쿼리 테스트")
    @Test
    @Transactional
    void updateViewCountTests() {
        // given
        ViewCountEntity savedViewCount = backUpRepository.save(
                ViewCountEntity.Init(1l, 0l)
        );

        entityManager.flush();
        entityManager.clear();

        int result1 = backUpRepository.updateViewCount(1l, 100l);
        int result2 = backUpRepository.updateViewCount(1l, 300l);
        int result3 = backUpRepository.updateViewCount(1l, 200l);

        Assertions.assertEquals(result1, 1);
        Assertions.assertEquals(result2, 1);
        Assertions.assertEquals(result3, 0);


    }

}