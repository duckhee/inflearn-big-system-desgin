package kr.co.won.articleread.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kr.co.won.dataserializer.DataSerializer;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@ToString
public class OptimizedCache {

    private String data;

    private LocalDateTime expiredAt;

    public static OptimizedCache of(Object data, Duration logicalTTL) {
        OptimizedCache optimizedCache = new OptimizedCache();
        optimizedCache.data = DataSerializer.serialized(data);
        optimizedCache.expiredAt = LocalDateTime.now().plus(logicalTTL);
        return optimizedCache;
    }

    @JsonIgnore
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public <T> T parseData(Class<T> clazz) {
        return DataSerializer.deserialize(data, clazz);
    }
}
