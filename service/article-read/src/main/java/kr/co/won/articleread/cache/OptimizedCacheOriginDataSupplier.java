package kr.co.won.articleread.cache;

@FunctionalInterface
public interface OptimizedCacheOriginDataSupplier<T> {

    T get() throws Throwable;
}
