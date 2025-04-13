package kr.co.won.articleread.cache;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Aspect
@Component
@RequiredArgsConstructor
public class OptimizedCacheAspect {

    private final OptimizedCacheManager optimizedCacheManager;

    /**
     * 최적화된 cache에 대해서 Annotation에 대한 처리를 하는 Aspect
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(OptimizedCacheable)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        OptimizedCacheable cacheable = findAnnotation(proceedingJoinPoint);
        return optimizedCacheManager.process(
                cacheable.type(),
                cacheable.ttlSeconds(),
                proceedingJoinPoint.getArgs(),
                findReturnType(proceedingJoinPoint),
                () -> proceedingJoinPoint.proceed()
        );
    }

    /**
     * 처리할 Annotation이 있는지 가져오는 함수
     * @param joinPoint
     * @return
     */
    private OptimizedCacheable findAnnotation(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        OptimizedCacheable annotation = methodSignature.getMethod().getAnnotation(OptimizedCacheable.class);
        return annotation;
    }

    /**
     * 함수에 대한 반환 값을 찾아주는 함수
     * @param joinPoint
     * @return
     */
    private Class<?> findReturnType(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        return methodSignature.getReturnType();
    }
}
