package kr.co.won.dataserializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSerializer {

    private final static ObjectMapper objectMapper = initialize();

    private static ObjectMapper initialize() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 역직렬화할 때 없는 필드 있으면 에러가 발생하는 것을 방지를 하기 위한 false 설정
    }

    /**
     * 데이터와 변환을 해줄 클래스를 인자로 받아서 변환을 해준다.
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T deserialize(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException exception) {
            log.error("[DataSerializer.deserialize] data={}, clazz={}", data, clazz, exception);
            return null;
        }
    }

    public static <T> T deserialize(Object obj, Class<T> clazz) {
        return objectMapper.convertValue(obj, clazz);
    }

    public static String serialized(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[DataSerializer.serialized] data={}", object.toString(), e);
            return null;
        }
    }
}
