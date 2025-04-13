package kr.co.won.articleread.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * File Name        : OptimizedCacheable<br/>
 * COMMENT          : 최적화된 캐시를 적용을 하기 위한 Annotation
 * </p>
 *
 * @author : Doukhee Won
 * @version : v0.0.1
 * @since : 2025. 4. 13.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OptimizedCacheable {

    /// cache 를 적용할 Method에 대해서 유일하게 관리를 하기 위한 값
    String type();

    /// 캐시에 대한 만료 시간을 인자로 받기 위한 값
    long ttlSeconds() default 0;
}
