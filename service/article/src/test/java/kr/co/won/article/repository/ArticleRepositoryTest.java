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

    @DisplayName(value = "03. infinity scroll First Call Tests")
    @Test
    void findFirstInfinityScrollTests() {
        List<ArticleEntity> firstInfinityScroll = articleRepository.findAllInfinityScroll(1l, 30l);
        assertEquals(firstInfinityScroll.size(), 30);
    }

    @DisplayName(value = "04. infinity scroll second call Tests")
    @Test
    void findSecondInfinityScrollTests() {
        List<ArticleEntity> firstInfinityScroll = articleRepository.findAllInfinityScroll(1l, 30l);
        assertEquals(firstInfinityScroll.size(), 30);
        ArticleEntity lastArticleEntity = firstInfinityScroll.get(firstInfinityScroll.size() - 1);
        List<ArticleEntity> allInfinityScroll = articleRepository.findAllInfinityScroll(1l, 30l, lastArticleEntity.getArticleId());
        for (ArticleEntity articleEntity : allInfinityScroll) {
            log.info("articleEntity: {}", articleEntity.getArticleId());
        }

    }
}