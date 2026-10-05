package kr.co.won.article.service.utils.paging;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 블록 제한 count 계산 테스트 [원본 코드 유지]
 * => 이 값이 HATEOAS 응답의 page.totalElements 로 사용된다. (DB 없이 실행 가능)
 */
class PageLimitCalculatorTest {

    @DisplayName(value = "01. calculate page limit size Tests")
    @Test
    void calculatePageLimitTests() {
        calculatePageLimit(1l, 30l, 10l, 301l);
        calculatePageLimit(7l, 30l, 10l, 301l);
        calculatePageLimit(10l, 30l, 10l, 301l);
        calculatePageLimit(11l, 30l, 10l, 601l);
        calculatePageLimit(12l, 30l, 10l, 601l);
    }

    void calculatePageLimit(Long page, Long pageSize, Long movablePageCount, Long expected) {
        Long resultPage = PageLimitCalculator.calculatePageLimit(page, pageSize, movablePageCount);
        Assertions.assertThat(resultPage).isEqualTo(expected);
    }

}