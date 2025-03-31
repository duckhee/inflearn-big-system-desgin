package kr.co.won.hotarticle.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeCalculatorUtils {

    /**
     * 자정까지 남은 시간을 구해 주는 함수
     *
     * @return {@link Duration}
     */
    public static Duration calculatorDurationToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        // 하루를 더해서 00시에 대한 값 가져오기
        LocalDateTime midnightTime = now.plusDays(1).with(LocalTime.MIDNIGHT);
        // 다음 날 00시까지 남은 시간을 Duration 객체로 반환을 한다.
        return Duration.between(now, midnightTime);
    }
}
