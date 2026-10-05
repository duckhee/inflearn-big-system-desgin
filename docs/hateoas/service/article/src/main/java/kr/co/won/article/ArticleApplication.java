package kr.co.won.article;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 게시글 서비스 (HATEOAS 마이그레이션 버전)
 *
 * [원본과의 차이]
 * - 원본은 이 클래스에 @EntityScan / @EnableJpaRepositories 를 직접 선언했다.
 * - HATEOAS 버전에서는 이를 {@link kr.co.won.article.configuration.JpaConfiguration} 으로 분리했다.
 *   => @WebMvcTest 같은 슬라이스 테스트는 메인 클래스의 어노테이션을 그대로 읽기 때문에,
 *      메인 클래스에 JPA 설정이 있으면 DB 없이 컨트롤러(HATEOAS 링크)만 테스트할 수 없다.
 *   => 별도 @Configuration 으로 분리하면 슬라이스 테스트에서는 제외되고, 실제 실행 시에는 동일하게 동작한다.
 */
@SpringBootApplication
public class ArticleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArticleApplication.class, args);
    }
}
