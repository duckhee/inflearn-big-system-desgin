package kr.co.won.articleread.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class LongToDoubleTests {

    @DisplayName(value = "01. long 값과 double 값 관련 유실 테스트")
    @Test
    void longToDoubleTests() {
        // long은 64bit로 정수 표현
        long longValue = 111_111_111_111_111_111l;
        System.out.println("long value = " + longValue);
        // double은 64bit 실수로 표현
        double doubleValue = longValue;
        System.out.println("double value = " + new BigDecimal(doubleValue).toString());
        long longValue2 = (long) doubleValue;
        System.out.println("longValue2 = " + longValue2);
    }
}
