package kr.co.won.hotarticle.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TimeCalculatorUtilsTest {

    @DisplayName(value = "01. 다음날 자정까지 남은 시간 구하는 유틸 함수 테스트")
    @Test
    public void calculateMidnightTests() {
        Duration duration = TimeCalculatorUtils.calculatorDurationToMidnight();
        System.out.println("duration.getSeconds()/60 = " + duration.getSeconds() / 60);
    }
}