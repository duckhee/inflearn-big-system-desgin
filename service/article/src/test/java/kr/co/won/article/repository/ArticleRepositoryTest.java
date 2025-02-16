package kr.co.won.article.repository;

import kr.co.won.article.entity.ArticleEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@DataJpaTest
@AutoConfigureDataJpa
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @DisplayName(value = "01. paging query Tests")
    @Test
    void findPagingTests() {
        List<ArticleEntity> findPagingResult = articleRepository.pagingQuery(1l, 1499970l, 30l);
        log.info("findPagingResult: {}", findPagingResult);
        for (ArticleEntity articleEntity : findPagingResult) {
            log.info("articleEntity: {}", articleEntity);
        }
    }

    @DisplayName(value = "02. paging number count Tests")
    @Test
    void countPageTests() {
        Long countPage = articleRepository.countPage(1l, 10000l);
        assertEquals(countPage, 10000l);
    }
}