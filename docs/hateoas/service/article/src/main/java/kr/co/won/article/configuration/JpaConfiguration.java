package kr.co.won.article.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Entity / Repository 스캔 설정
 *
 * - basePackages 를 "kr.co.won" 으로 잡는 이유:
 *   common:outbox-message-relay 모듈의 OutboxEntity / OutboxRepository (kr.co.won.common.*) 까지 함께 스캔해야 하기 때문 (원본과 동일)
 * - 원본에서는 ArticleApplication 에 선언되어 있던 설정을 그대로 옮겨온 것이다.
 *   => 메인 클래스에서 분리하면 @WebMvcTest 슬라이스 테스트 시 JPA 설정이 로딩되지 않는다.
 */
@Configuration
@EntityScan(basePackages = "kr.co.won")
@EnableJpaRepositories(basePackages = "kr.co.won")
public class JpaConfiguration {
}
