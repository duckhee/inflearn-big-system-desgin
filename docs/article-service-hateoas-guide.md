# Article Service HATEOAS 전환 가이드

> 대상 모듈: `service/article` (port 9000)
> 기준 버전: Spring Boot 3.4.2 / Spring Data Commons 3.4.2 / Spring HATEOAS 2.4.x / Java 17
> 작성일: 2026-10-05
> 실행 가능한 마이그레이션 프로젝트: [`docs/hateoas`](hateoas/README.md)

---

## 목차

1. [개요](#1-개요)
2. [현재 구조 분석](#2-현재-구조-분석)
3. [의사결정 기록 (ADR)](#3-의사결정-기록-adr)
4. [변경 후 전체 구조](#4-변경-후-전체-구조)
5. [변경 코드 전체](#5-변경-코드-전체)
6. [Paging 처리 상세](#6-paging-처리-상세)
7. [무한 스크롤(Cursor) 처리 상세](#7-무한-스크롤cursor-처리-상세)
8. [API 응답 예시](#8-api-응답-예시)
9. [테스트 코드](#9-테스트-코드)
10. [주의 사항 / 함정](#10-주의-사항--함정)
11. [부록 A. v1을 직접 HATEOAS로 바꿀 경우 수정해야 하는 클라이언트 코드](#부록-a-v1을-직접-hateoas로-바꿀-경우-수정해야-하는-클라이언트-코드)
12. [부록 B. 확장 아이디어](#부록-b-확장-아이디어)

---

## 1. 개요

### 1.1 HATEOAS란?

**HATEOAS(Hypermedia As The Engine Of Application State)** 는 REST 성숙도 모델(Richardson Maturity Model)의 최상위(Level 3) 단계입니다.
서버가 응답에 **"다음에 할 수 있는 행동"을 링크(`_links`)로 함께 내려주어**, 클라이언트가 URL을 하드코딩하지 않고 링크를 따라가며 상태를 전이하도록 합니다.

```json
{
  "articleId": 1,
  "title": "hello",
  "_links": {
    "self":     { "href": "http://localhost:9000/api/v2/articles/1" },
    "articles": { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=1&size=10" }
  }
}
```

### 1.2 이 문서의 목표

- Article 서비스의 모든 조회/생성/수정/삭제 API를 **HAL(`application/hal+json`) 형식의 HATEOAS 응답**으로 제공한다.
- **페이지 번호 방식 Paging**은 Spring Data의 `PagedResourcesAssembler`를 이용해 `first / prev / self / next / last` 링크와 `page` 메타데이터를 자동 생성한다.
- **무한 스크롤(Cursor) 방식**은 `CollectionModel` + 직접 생성한 `next` 링크로 표현한다.
- 기존 Service / Repository / 쿼리(커버링 인덱스, 페이지 제한 count) 로직은 **변경하지 않는다.**
- 기존 `/api/articles` (v1)를 사용하는 내부 서비스(`article-read`, `hot-article`)는 **깨지지 않아야 한다.**

---

## 2. 현재 구조 분석

### 2.1 현재 API 목록 (`ArticleController`)

| Method | URL | 파라미터 | 응답 타입 |
|--------|-----|----------|-----------|
| GET | `/api/articles` | `boardId`, `page`(1-based), `size` | `ArticlePageResponse { articles, articleCount }` |
| GET | `/api/articles/infinity-scroll` | `boardId`, `size`, `lastArticleId`(optional) | `List<ArticleResponse>` |
| GET | `/api/articles/{articleId}` | - | `ArticleResponse` |
| GET | `/api/articles/boards/{boardId}/article-count` | - | `Long` |
| POST | `/api/articles` | body: `ArticleCreateRequest` | `ArticleResponse` |
| PUT | `/api/articles/{articleId}` | body: `ArticleUpdateRequest` | `ArticleResponse` |
| DELETE | `/api/articles/{articleId}` | - | `void` |

### 2.2 현재 Paging 동작 (`ArticleService.pageArticle`)

```java
public ArticlePageResponse pageArticle(Long boardId, Long pageNumber, Long pageSize) {
    Long pageLimitCount = PageLimitCalculator.calculatePageLimit(pageNumber, pageSize, 10L);
    Long countPage = articleRepository.countPage(boardId, pageLimitCount);
    long pageOffset = (pageNumber - 1) * pageSize;
    List<ArticleResponse> pageArticleResponse = articleRepository.pagingQuery(boardId, pageOffset, pageSize).stream()
            .map(ArticleResponse::fromEntity).toList();
    return ArticlePageResponse.of(pageArticleResponse, countPage);
}
```

핵심 특징 3가지:

1. **page 번호는 1부터 시작** (`offset = (pageNumber - 1) * pageSize`)
2. **`articleCount`는 전체 게시글 수가 아니다.**
   `PageLimitCalculator.calculatePageLimit(page, size, 10)` = `(((page - 1) / 10) + 1) * size * 10 + 1`
   → "현재 페이지가 속한 10개 페이지 블록까지 + 1개"만 count 한다 (대용량 테이블에서 `COUNT(*)` 전체 스캔을 피하기 위한 설계).

   | 요청 page (size=10) | limit | 의미 |
   |---------------------|-------|------|
   | 1 ~ 10 | 101 | 1~10 페이지 + "다음 블록 존재 여부" 1건 |
   | 11 ~ 20 | 201 | 1~20 페이지 + 1건 |
   | 21 ~ 30 | 301 | 1~30 페이지 + 1건 |

3. 실제 데이터 조회는 **커버링 인덱스 서브쿼리**(`pagingQuery`)로 수행한다.

### 2.3 현재 무한 스크롤 동작 (`ArticleService.infinityScrollArticle`)

- `lastArticleId == null` → 최신 글부터 `size` 개
- `lastArticleId != null` → `article_id < lastArticleId` 조건으로 `size` 개 (Cursor 방식)
- 전체 페이지 수 / 전체 개수 개념이 **없다.**

### 2.4 이 API를 사용하는 외부 모듈 (변경 시 영향 범위)

| 사용처 | 호출 API | 역직렬화 타입 |
|--------|----------|---------------|
| `service/article-read/.../client/ArticleClient.java` | `GET /api/articles/{id}` | `ArticleClient.ArticleResponse` |
| 〃 | `GET /api/articles?boardId&page&size` | `ArticleClient.ArticlePageResponse` |
| 〃 | `GET /api/articles/infinity-scroll` | `List<ArticleResponse>` |
| 〃 | `GET /api/articles/boards/{boardId}/article-count` | `Long` |
| `service/hot-article/.../client/ArticleClient.java` | `GET /api/articles/{id}` | `ArticleResponse` |
| `service/article-read`, `service/hot-article` 의 `DataInitializer` (test) | `POST /api/articles` | - |
| `service/article/.../ArticleControllerTest` (test) | v1 전체 | v1 DTO |

> ⚠️ `/api/articles`의 응답 형식을 HAL로 바꾸면 위 클라이언트가 **모두 역직렬화 실패**합니다.
> (`articles` 필드가 `_embedded.articles`로 이동, 단건 응답에 `_links` 추가 등)

---

## 3. 의사결정 기록 (ADR)

### 3.1 팀 회의 요약

| 발언자 | 역할 | 의견 |
|--------|------|------|
| 톰 | 요청 분석 & 기획 | 목표는 "Article API를 HATEOAS로 제공 + Paging 링크 자동화". 내부 서비스 호환성은 비기능 요구사항으로 반드시 지켜야 함 |
| 샘 | 전문 지식 | `PagedResourcesAssembler`는 spring-data-commons(`org.springframework.data.web`)에 있고, `spring-boot-starter-hateoas` 추가 시 Boot 자동 설정이 Bean으로 등록함. Article 서비스는 이미 `spring-boot-starter-data-jpa`가 있어 바로 사용 가능 |
| 제니퍼 | 백엔드 시니어 | `articleCount`는 "블록 제한 count"이므로 `totalElements`에 넣으면 `last` 링크가 "다음 블록의 첫 페이지"를 가리킴. 버그가 아니라 설계 의도이므로 문서화 필수 |
| 후니 | 프런트엔드 | 프런트는 `_links.next.href`만 따라가면 됨. 목록 키 이름을 `@Relation(collectionRelation = "articles")`로 고정해 달라 |
| 쏘니 | Java/Spring 시니어 | `one-indexed-parameters: true` 설정 시 링크의 `page=`와 `page.number` 메타데이터 모두 1-based로 맞춰짐(3.4.2 소스로 확인). 컨트롤러는 `Pageable`로 받고, 서비스에는 `pageNumber + 1`을 넘기면 서비스 무변경 |
| 쿤 | 임베디드 시니어 | `size` 무제한 요청 방어 필요 → `max-page-size: 50` 설정 |
| 쿠쿤 | 파이썬 시니어 | Cursor 방식은 Page 개념이 없으므로 `PagedModel` 말고 `CollectionModel` + `next` 링크. "결과 개수 == size 이면 next 있음" 규칙이 가장 단순 |
| 셔럼 | C# 시니어 | v1 경로를 직접 바꾸면 `article-read`, `hot-article` 클라이언트가 깨짐 → **`/api/v2/articles`로 분리** 하자 |
| 시노 | 하드웨어 시니어 | 인터페이스 규격(v1)은 유지하고 어댑터(Assembler) 계층만 추가하는 구조가 안전. `PageImpl`이 total을 보정하는 동작은 테스트로 확인 |
| 윌리엄 | 팀 리더 & 최종 검수 | **결정: v2 HATEOAS 컨트롤러 신설 + Paging은 `PagedResourcesAssembler`, Cursor는 `CollectionModel`. v1은 그대로 유지.** |

### 3.2 검토한 대안

| 대안 | 설명 | 장점 | 단점 | 채택 |
|------|------|------|------|------|
| A. v1 경로 직접 변환 | `/api/articles` 응답을 HAL로 교체 | URL 하나로 통일 | `article-read`/`hot-article` 클라이언트·테스트 전부 수정 필요, 배포 순서 의존 | ❌ (부록 A 참고) |
| **B. v2 경로 신설** | `/api/v2/articles`에 HATEOAS 컨트롤러 추가 | 내부 서비스 무영향, 점진적 전환 가능, 롤백 쉬움 | 컨트롤러 2개 공존 (유지보수 대상 증가) | ✅ |
| C. Content Negotiation | 같은 URL에서 `Accept: application/hal+json` 일 때만 HAL | URL 통일 + 호환 | Boot 기본값(`use-hal-as-default-json-media-type=true`) 때문에 `application/json` 요청도 HAL이 될 수 있어 설정/검증이 까다로움, 컨트롤러 메서드 중복 | ❌ |

### 3.3 Paging 처리 방식 결정

| 방식 | 채택 | 이유 |
|------|------|------|
| `PageImpl` + `PagedResourcesAssembler` (링크 자동 생성) | ✅ 페이지 번호 방식 | 링크 생성 코드 0줄, 현재 요청 URL의 다른 파라미터(`boardId`) 자동 유지, 1-based 설정 지원 |
| 직접 `PagedModel.of(content, metadata, links)` 조립 | ❌ | 링크 조립 코드를 직접 유지해야 함 |
| `CollectionModel` + 수동 `next` 링크 | ✅ 무한 스크롤 | Cursor 방식은 `Page`로 표현 불가 (`SlicedResourcesAssembler`도 page 번호 기반이라 부적합) |

---

## 4. 변경 후 전체 구조

### 4.1 파일 변경 목록

| 구분 | 파일 | 내용 |
|------|------|------|
| 수정 | `service/article/build.gradle` | `spring-boot-starter-hateoas` 의존성 추가 |
| 수정 | `service/article/src/main/resources/application.yml` | `spring.data.web.pageable.*` 설정 추가 |
| 수정 | `service/article/.../service/response/ArticleResponse.java` | `@Relation` 추가 (HAL 컬렉션 키 이름 고정) |
| 신규 | `service/article/.../service/response/BoardArticleCountResponse.java` | 게시판 글 수 응답 DTO (`Long` 은 링크를 붙일 수 없으므로) |
| 신규 | `service/article/.../controller/hateoas/ArticleModelAssembler.java` | 단건 `ArticleResponse` → `EntityModel` 변환 + 링크 |
| 신규 | `service/article/.../controller/hateoas/package-info.java` | 패키지 문서 |
| 신규 | `service/article/.../controller/ArticleHateoasController.java` | `/api/v2/articles` HATEOAS API |
| 신규 | `service/article/src/test/.../controller/ArticleHateoasControllerTest.java` | v2 API 테스트 |
| **무변경** | `ArticleController`, `ArticleService`, `ArticleRepository`, `PageLimitCalculator`, `ArticlePageResponse` | 기존 v1 로직 그대로 사용 |
| **무변경** | `article-read`, `hot-article` 의 `ArticleClient` | v1 계속 사용 |

### 4.2 요청 흐름

```
[Client]
   │  GET /api/v2/articles?boardId=1&page=3&size=10
   ▼
[ArticleHateoasController]
   │  Pageable 로 수신 (one-indexed → pageable.getPageNumber() == 2)
   │  service 에는 1-based 로 변환해서 전달 (2 + 1 = 3)
   ▼
[ArticleService.pageArticle(boardId, 3, 10)]          ← 기존 코드 그대로
   │  PageLimitCalculator → countPage → pagingQuery
   ▼
ArticlePageResponse { articles(10건), articleCount(101) }
   │
   ▼
[ArticleHateoasController]
   │  new PageImpl<>(articles, pageable, articleCount)
   │  pagedResourcesAssembler.toModel(page, articleModelAssembler)
   ▼
PagedModel<EntityModel<ArticleResponse>>
   │  _embedded.articles[*]._links.self
   │  _links.first / prev / self / next / last
   │  page { size, totalElements, totalPages, number }
   ▼
[Client]  application/hal+json
```

---

## 5. 변경 코드 전체

> 패키지 루트: `service/article/src/main/java/kr/co/won/article`

### 5.1 `service/article/build.gradle` (수정)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    // HATEOAS: RepresentationModel / EntityModel / PagedModel / WebMvcLinkBuilder 제공
    // spring-data-commons 와 함께 있으면 PagedResourcesAssembler Bean 이 자동 등록된다.
    implementation 'org.springframework.boot:spring-boot-starter-hateoas'
//    implementation 'mysql:mysql-connector-java'
//    implementation 'com.h2database:h2'
    implementation project(':common:snowflake')
    implementation project(':common:outbox-message-relay')
    implementation project(":common:event")

    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'com.h2database:h2'
}
```

**설명**

- `spring-boot-starter-hateoas`는 `spring-hateoas` + `spring-plugin-core` + Boot의 `HypermediaAutoConfiguration`을 가져옵니다.
- `SpringDataWebAutoConfiguration`(이미 data-jpa로 활성화)이 HATEOAS를 감지하면 `HateoasAwareSpringDataWebConfiguration`이 적용되어
  `PagedResourcesAssembler<T>`와 `HateoasPageableHandlerMethodArgumentResolver` Bean이 등록됩니다.
- 기존 v1 컨트롤러는 `RepresentationModel`을 반환하지 않으므로 응답 형식에 **아무 영향이 없습니다.**

### 5.2 `service/article/src/main/resources/application.yml` (수정)

```yaml
server:
  port: 9000
  shutdown: graceful

spring:
  config:
    import: optional:file:.env[.properties] # env file
  #    import: optional:application-db.yml # h2 database
  application:
    name: board-article-service
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${ARTICLE_DB_URL}
    username: ${ARTICLE_DB_USER}
    password: ${ARTICLE_DB_PASSWORD}

  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    open-in-view: false
    show-sql: true
    hibernate:
      ddl-auto: none
    database: mysql
    mapping-resources:
      - META-INF/orm.xml
  data:
    redis:
      host: ${HOT_ARTICLE_REDIS_URL}
      port: 6379
      password: ${HOT_ARTICLE_REDIS_PASSWORD}
    # ===== HATEOAS Paging 설정 (Pageable 파라미터 해석 규칙) =====
    web:
      pageable:
        one-indexed-parameters: true # ?page=1 을 첫 페이지로 해석 (기존 v1 API 와 동일한 1-based)
        page-parameter: page         # 쿼리 파라미터 이름 (기존 API 와 동일)
        size-parameter: size         # 쿼리 파라미터 이름 (기존 API 와 동일)
        default-page-size: 10        # size 미지정 시 기본값
        max-page-size: 50            # size 상한 (과도한 요청 방어, 초과 시 50 으로 강제)
  kafka:
    bootstrap-servers: ${KAFKA_SERVER_URL}
```

**설명**

| 설정 | 기본값 | 변경값 | 이유 |
|------|--------|--------|------|
| `one-indexed-parameters` | `false` | `true` | 기존 API가 `page=1`을 첫 페이지로 사용. `true`로 해야 `Pageable` 해석, 생성되는 링크의 `page=`, `page.number` 메타데이터가 모두 1-based로 일치 |
| `page-parameter` / `size-parameter` | `page` / `size` | 동일 | 명시적으로 적어 두어 기존 API와 이름이 같다는 것을 드러냄 |
| `default-page-size` | `20` | `10` | 기존 테스트/화면 기준 10건 |
| `max-page-size` | `2000` | `50` | `size=100000` 같은 요청으로 DB 부하가 생기는 것을 방지 |

> 이 설정은 `Pageable` 타입 파라미터에만 적용됩니다. v1 컨트롤러는 `@RequestParam Long page`를 쓰므로 영향이 없습니다.

### 5.3 `service/response/ArticleResponse.java` (수정)

```java
package kr.co.won.article.service.response;

import kr.co.won.article.entity.ArticleEntity;
import lombok.Getter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

@Getter
@ToString
@Relation(itemRelation = "article", collectionRelation = "articles") // HAL _embedded 의 key 이름 고정
public class ArticleResponse {

    private Long articleId;

    private String title;

    private String content;

    private Long boardId; // shard Key

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    public static ArticleResponse fromEntity(ArticleEntity articleEntity) {
        ArticleResponse articleResponse = new ArticleResponse();
        articleResponse.articleId = articleEntity.getArticleId();
        articleResponse.title = articleEntity.getTitle();
        articleResponse.content = articleEntity.getContent();
        articleResponse.boardId = articleEntity.getBoardId();
        articleResponse.writerId = articleEntity.getWriterId();
        articleResponse.createdAt = articleEntity.getCreatedAt();
        articleResponse.modifiedAt = articleEntity.getModifiedAt();
        return articleResponse;
    }
}
```

**설명**

- `@Relation`이 없으면 HAL은 클래스 이름으로 키를 만들어 `_embedded.articleResponseList`가 됩니다.
- `collectionRelation = "articles"`로 지정하면 `_embedded.articles`가 되어 v1의 `articles` 필드명과 의미가 이어집니다.
- 기존 import 중 사용하지 않던 `ArticleRepository` import는 제거했습니다.
- `@Relation`은 메타데이터일 뿐이라 v1 JSON 직렬화에는 영향이 없습니다.

### 5.4 `service/response/BoardArticleCountResponse.java` (신규)

```java
package kr.co.won.article.service.response;

import lombok.Getter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

/**
 * 게시판 별 게시글 수 응답
 * => v1 은 Long 을 그대로 반환하지만, HATEOAS 에서는 링크를 붙일 객체가 필요하므로 DTO 로 감싼다.
 */
@Getter
@ToString
@Relation(itemRelation = "boardArticleCount")
public class BoardArticleCountResponse {

    private Long boardId;

    private Long articleCount;

    protected BoardArticleCountResponse() {
    }

    public static BoardArticleCountResponse of(Long boardId, Long articleCount) {
        BoardArticleCountResponse response = new BoardArticleCountResponse();
        response.boardId = boardId;
        response.articleCount = articleCount;
        return response;
    }
}
```

**설명**

- `Long`은 `final` 클래스이자 단순 값이라 `EntityModel.of(Long)`으로 감싸면 HAL 출력이 어색하고 의미도 없습니다.
- `boardId`를 함께 담아 응답만으로 어떤 게시판의 count인지 알 수 있게 했습니다.

### 5.5 `controller/hateoas/package-info.java` (신규)

```java
/**
 * HATEOAS 응답(RepresentationModel)을 만들기 위한 Assembler 모음
 * => Service 의 응답 DTO 에 링크(_links)를 붙이는 역할만 담당한다. (비즈니스 로직 X)
 */
package kr.co.won.article.controller.hateoas;
```

### 5.6 `controller/hateoas/ArticleModelAssembler.java` (신규)

```java
package kr.co.won.article.controller.hateoas;

import kr.co.won.article.controller.ArticleHateoasController;
import kr.co.won.article.service.response.ArticleResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * ArticleResponse -> EntityModel<ArticleResponse> 변환
 * => 단건 조회, 생성, 수정, 목록(_embedded) 의 각 항목에 공통으로 사용한다.
 */
@Component
public class ArticleModelAssembler implements RepresentationModelAssembler<ArticleResponse, EntityModel<ArticleResponse>> {

    /** 게시글이 속한 게시판 목록 링크에 사용할 기본 페이지 크기 */
    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Override
    public EntityModel<ArticleResponse> toModel(ArticleResponse article) {
        return EntityModel.of(article,
                // 자기 자신 : GET /api/v2/articles/{articleId}
                linkTo(methodOn(ArticleHateoasController.class).readArticle(article.getArticleId())).withSelfRel(),
                // 게시글이 속한 게시판의 첫 페이지 : GET /api/v2/articles?boardId={boardId}&page=1&size=10
                boardArticlesLink(article.getBoardId()),
                // 게시판 게시글 수 : GET /api/v2/articles/boards/{boardId}/article-count
                linkTo(methodOn(ArticleHateoasController.class).boardArticleCount(article.getBoardId())).withRel("board-article-count")
        );
    }

    /**
     * Pageable 파라미터를 가진 메서드는 methodOn 대신 UriComponentsBuilder 로 직접 쿼리 파라미터를 만든다.
     * => one-indexed 설정과 무관하게 항상 page=1 을 명확히 표현하기 위함
     */
    private Link boardArticlesLink(Long boardId) {
        String href = linkTo(ArticleHateoasController.class).toUriComponentsBuilder()
                .queryParam("boardId", boardId)
                .queryParam("page", 1)
                .queryParam("size", DEFAULT_PAGE_SIZE)
                .build()
                .toUriString();
        return Link.of(href, "articles");
    }
}
```

**설명**

- `RepresentationModelAssembler<T, R>`를 구현하면 `PagedResourcesAssembler.toModel(page, assembler)`에 그대로 넘길 수 있어, **목록의 각 항목에도 같은 링크 규칙이 적용**됩니다.
- `linkTo(methodOn(Controller.class).method(args))`는 컨트롤러 매핑 정보를 읽어 URL을 만듭니다. URL 문자열 하드코딩이 사라져 경로가 바뀌어도 링크가 자동으로 따라갑니다.
  - `methodOn`은 컨트롤러 메서드의 **반환 타입을 프록시**로 만들기 때문에, 링크 대상 메서드는 `final`이 아닌 타입(`EntityModel`, `PagedModel`, `CollectionModel`, `ResponseEntity`)을 반환해야 합니다. (v2 컨트롤러가 `Long`을 반환하지 않는 이유 중 하나)
- `Pageable` 파라미터를 받는 메서드에 `methodOn`을 쓰면 `Pageable` → 쿼리 파라미터 변환이 컨텍스트 설정에 따라 달라질 수 있습니다. 그래서 `boardArticlesLink`는 `UriComponentsBuilder`로 명시적으로 만들었습니다.

### 5.7 `controller/ArticleHateoasController.java` (신규)

```java
package kr.co.won.article.controller;

import kr.co.won.article.controller.hateoas.ArticleModelAssembler;
import kr.co.won.article.service.ArticleService;
import kr.co.won.article.service.request.ArticleCreateRequest;
import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import kr.co.won.article.service.response.BoardArticleCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * HATEOAS(HAL) 형식의 게시글 API
 * => 기존 /api/articles (v1) 은 내부 서비스(article-read, hot-article)가 사용하므로 그대로 두고,
 * 외부 클라이언트용으로 /api/v2/articles 를 제공한다.
 * => Service 로직은 v1 과 동일한 것을 사용하고, 응답에 링크만 추가한다.
 */
@RestController
@RequestMapping(path = "/api/v2/articles")
@RequiredArgsConstructor
public class ArticleHateoasController {

    private final ArticleService articleService;
    private final ArticleModelAssembler articleModelAssembler;
    private final PagedResourcesAssembler<ArticleResponse> pagedResourcesAssembler;

    /**
     * 페이지 번호 방식 목록 조회
     * ex) GET /api/v2/articles?boardId=1&page=1&size=10
     *
     * @param boardId  게시판 ID
     * @param pageable one-indexed 설정에 의해 page=1 -> pageable.getPageNumber() == 0
     */
    @GetMapping
    public PagedModel<EntityModel<ArticleResponse>> pageArticles(@RequestParam("boardId") Long boardId, Pageable pageable) {
        // 1. 기존 Service 는 1-based page 를 사용하므로 변환해서 전달한다.
        long pageNumber = pageable.getPageNumber() + 1L;
        long pageSize = pageable.getPageSize();
        ArticlePageResponse response = articleService.pageArticle(boardId, pageNumber, pageSize);

        // 2. 기존 응답(목록 + 블록 제한 count)을 Spring Data Page 로 감싼다.
        //    articleCount 는 "현재 10페이지 블록 + 1" 까지의 count 이므로 totalPages 도 블록 기준이 된다.
        Page<ArticleResponse> articlePage = new PageImpl<>(response.getArticles(), pageable, response.getArticleCount());

        // 3. 데이터가 없을 때 _embedded 가 사라지지 않도록 빈 모델을 만든다.
        if (articlePage.isEmpty()) {
            @SuppressWarnings("unchecked")
            PagedModel<EntityModel<ArticleResponse>> emptyModel =
                    (PagedModel<EntityModel<ArticleResponse>>) pagedResourcesAssembler.toEmptyModel(articlePage, ArticleResponse.class);
            return emptyModel;
        }

        // 4. first / prev / self / next / last 링크 + page 메타데이터 자동 생성
        //    각 항목은 ArticleModelAssembler 로 self 등 링크를 붙인다.
        return pagedResourcesAssembler.toModel(articlePage, articleModelAssembler);
    }

    /**
     * 무한 스크롤(Cursor) 방식 목록 조회
     * ex) GET /api/v2/articles/infinity-scroll?boardId=1&size=10
     * ex) GET /api/v2/articles/infinity-scroll?boardId=1&size=10&lastArticleId=123
     */
    @GetMapping(path = "/infinity-scroll")
    public CollectionModel<EntityModel<ArticleResponse>> infinityScrollArticles(@RequestParam("boardId") Long boardId,
                                                                                @RequestParam("size") Long pageSize,
                                                                                @RequestParam(value = "lastArticleId", required = false) Long lastArticleId) {
        List<ArticleResponse> articles = articleService.infinityScrollArticle(boardId, pageSize, lastArticleId);

        List<Link> links = new ArrayList<>();
        // 현재 요청 그대로 / 처음부터 다시 (cursor 없음)
        // => null 인 선택 파라미터는 "{&lastArticleId}" 템플릿 변수로 남으므로 expand() 로 제거한다.
        links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, lastArticleId)).withSelfRel().expand());
        links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, null)).withRel(IanaLinkRelations.FIRST).expand());
        // 요청한 개수만큼 가득 찼을 때만 다음 데이터가 있다고 판단 -> 마지막 게시글 ID 를 다음 cursor 로 사용
        if (!articles.isEmpty() && articles.size() == pageSize) {
            Long nextCursor = articles.get(articles.size() - 1).getArticleId();
            links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, nextCursor)).withRel(IanaLinkRelations.NEXT));
        }

        CollectionModel<EntityModel<ArticleResponse>> model = articleModelAssembler.toCollectionModel(articles);
        return model.add(links);
    }

    /**
     * 단건 조회
     * ex) GET /api/v2/articles/{articleId}
     */
    @GetMapping(path = "/{articleId}")
    public EntityModel<ArticleResponse> readArticle(@PathVariable("articleId") Long articleId) {
        return articleModelAssembler.toModel(articleService.findArticle(articleId));
    }

    /**
     * 게시판 별 게시글 수 조회
     * ex) GET /api/v2/articles/boards/{boardId}/article-count
     */
    @GetMapping(path = "/boards/{boardId}/article-count")
    public EntityModel<BoardArticleCountResponse> boardArticleCount(@PathVariable("boardId") Long boardId) {
        BoardArticleCountResponse response = BoardArticleCountResponse.of(boardId, articleService.articleCountNumber(boardId));
        return EntityModel.of(response,
                linkTo(methodOn(ArticleHateoasController.class).boardArticleCount(boardId)).withSelfRel(),
                Link.of(linkTo(ArticleHateoasController.class).toUriComponentsBuilder()
                        .queryParam("boardId", boardId)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build().toUriString(), "articles")
        );
    }

    /**
     * 게시글 생성
     * => 201 Created + Location 헤더 (생성된 리소스의 self 링크)
     */
    @PostMapping
    public ResponseEntity<EntityModel<ArticleResponse>> createArticle(@RequestBody ArticleCreateRequest request) {
        EntityModel<ArticleResponse> model = articleModelAssembler.toModel(articleService.createArticle(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    /**
     * 게시글 수정
     */
    @PutMapping(path = "/{articleId}")
    public EntityModel<ArticleResponse> updateArticle(@PathVariable("articleId") Long articleId, @RequestBody ArticleUpdateRequest request) {
        return articleModelAssembler.toModel(articleService.updateArticle(articleId, request));
    }

    /**
     * 게시글 삭제
     * => 204 No Content
     */
    @DeleteMapping(path = "/{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("articleId") Long articleId) {
        articleService.deleteArticle(articleId);
        return ResponseEntity.noContent().build();
    }
}
```

**설명 (메서드별)**

| 메서드 | 반환 타입 | 포인트 |
|--------|-----------|--------|
| `pageArticles` | `PagedModel<EntityModel<ArticleResponse>>` | `Pageable` 수신 → 1-based 변환 → 기존 Service 호출 → `PageImpl` 래핑 → `PagedResourcesAssembler` 가 링크/메타데이터 생성 ([6장](#6-paging-처리-상세)) |
| `infinityScrollArticles` | `CollectionModel<EntityModel<ArticleResponse>>` | Cursor 방식은 page 개념이 없으므로 `self / first / next` 만 직접 생성 ([7장](#7-무한-스크롤cursor-처리-상세)) |
| `readArticle` | `EntityModel<ArticleResponse>` | Assembler 로 `self`, `articles`, `board-article-count` 링크 |
| `boardArticleCount` | `EntityModel<BoardArticleCountResponse>` | `Long` 대신 DTO 로 감싸 링크 부착 |
| `createArticle` | `ResponseEntity<EntityModel<...>>` | REST 규약대로 `201 Created` + `Location` 헤더 |
| `updateArticle` | `EntityModel<ArticleResponse>` | 수정 결과 + 링크 |
| `deleteArticle` | `ResponseEntity<Void>` | `204 No Content` (삭제된 리소스에는 링크를 줄 수 없음) |

- `@PathVariable("articleId")` 처럼 이름을 명시한 이유: `methodOn` 링크 생성과 `-parameters` 컴파일 옵션 유무에 상관없이 파라미터 매핑을 확실히 하기 위함입니다.
- `PagedResourcesAssembler<ArticleResponse>`는 제네릭 타입이 달라도 같은 Bean이 주입됩니다 (Spring Data가 제네릭 무관 Bean 1개를 등록).

---

## 6. Paging 처리 상세

### 6.1 `Pageable` 해석 (요청 → 객체)

`HateoasPageableHandlerMethodArgumentResolver`가 쿼리 파라미터를 `Pageable`로 변환합니다.

| 요청 | `one-indexed-parameters: true` 일 때 `pageable.getPageNumber()` | `getPageSize()` | `getOffset()` |
|------|------------------|-----------------|---------------|
| `?page=1&size=10` | 0 | 10 | 0 |
| `?page=3&size=10` | 2 | 10 | 20 |
| `?page=0&size=10` | 0 (음수는 0으로 보정) | 10 | 0 |
| `?size=10` (page 생략) | 0 | 10 | 0 |
| `?page=2` (size 생략) | 1 | 10 (`default-page-size`) | 10 |
| `?page=1&size=999` | 0 | 50 (`max-page-size`로 제한) | 0 |

컨트롤러에서 `pageable.getPageNumber() + 1`을 Service에 넘기므로 Service 입장에서는 **기존 v1과 완전히 같은 값**을 받습니다.

```
v1: ?page=3&size=10 → pageArticle(boardId, 3, 10)
v2: ?page=3&size=10 → pageable(2, 10) → pageArticle(boardId, 2 + 1, 10) = pageArticle(boardId, 3, 10)
```

### 6.2 `PageImpl` 생성과 total 보정

```java
Page<ArticleResponse> articlePage = new PageImpl<>(response.getArticles(), pageable, response.getArticleCount());
```

`PageImpl` 생성자(spring-data-commons 3.4.2)는 다음과 같이 total을 **보정**합니다.

```java
this.total = pageable.toOptional().filter(it -> !content.isEmpty())
        .filter(it -> it.getOffset() + it.getPageSize() > total)
        .map(it -> it.getOffset() + content.size())
        .orElse(total);
```

→ "내용이 있고, 현재 페이지 끝(offset + size)이 total을 넘으면, total = offset + 실제 개수"
→ 즉 **마지막 페이지에서는 실제 개수로 맞춰지고**, 그 외에는 전달한 `articleCount`를 그대로 사용합니다.

### 6.3 블록 제한 count와 `totalPages` / `last` 링크의 의미

`totalPages = ceil(totalElements / size)` 이므로 블록 제한 count가 그대로 반영됩니다.

**예시: board 1에 게시글 1,000건, size=10**

| 요청 page | limit (calculatePageLimit) | totalElements | totalPages | `next` | `last` |
|-----------|---------------------------|---------------|------------|--------|--------|
| 1 | 101 | 101 | 11 | page=2 | page=11 |
| 10 | 101 | 101 | 11 | page=11 | page=11 |
| 11 | 201 | 201 | 21 | page=12 | page=21 |
| 100 | 1001 → 실제 1000 | 1000 | 100 | 없음 | page=100 |

**예시: board 2에 게시글 35건, size=10**

| 요청 page | totalElements | totalPages | `prev` | `next` | `last` |
|-----------|---------------|------------|--------|--------|--------|
| 1 | 35 | 4 | 없음 | page=2 | page=4 |
| 4 | 35 (보정 30+5) | 4 | page=3 | 없음 | page=4 |

> 📌 **중요**: 게시글이 많은 게시판에서 `last`는 "진짜 마지막 페이지"가 아니라 **"다음 블록의 첫 페이지"** 입니다.
> 이는 기존 v1의 "10개 페이지 번호 + 다음 버튼" UI를 위한 설계를 그대로 표현한 것입니다.
> 프런트엔드는 `page.totalPages`를 "현재 블록 기준 이동 가능한 최대 페이지"로 해석해야 합니다.

### 6.4 링크 생성 규칙 (`PagedResourcesAssembler` 내부 동작)

`page.hasPrevious()`는 `number > 0`, `page.hasNext()`는 `number + 1 < totalPages` 입니다.

| 링크 | 생성 조건 | href 예시 (page=3) |
|------|-----------|--------------------|
| `first` | `hasPrevious() \|\| hasNext()` (페이지가 2개 이상) | `?boardId=1&page=1&size=10` |
| `prev` | `hasPrevious()` | `?boardId=1&page=2&size=10` |
| `self` | 항상 | `?boardId=1&page=3&size=10` |
| `next` | `hasNext()` | `?boardId=1&page=4&size=10` |
| `last` | `first`와 동일 조건 | `?boardId=1&page=11&size=10` |

- **기준 URL은 현재 요청 URL** (`ServletUriComponentsBuilder.fromCurrentRequest()`)입니다.
  그래서 `boardId=1`처럼 Pageable이 아닌 파라미터도 자동으로 유지되고, `page`/`size`만 교체(`replaceQueryParam`)됩니다.
  - ⚠️ MockMvc 테스트에서 `.param("boardId", "1")`로 요청하면 `request.getQueryString()`이 비어 있어 링크에서 `boardId`가 빠진 것처럼 보입니다. 테스트에서는 `get("/api/v2/articles?boardId=1&page=1")`처럼 URI에 직접 써야 합니다.
- `one-indexed-parameters: true`이면 링크의 `page=` 값에 자동으로 `+1`이 적용됩니다.
- 정렬(`sort`)이 없으므로 `sort` 파라미터는 링크에 붙지 않습니다.
- 페이지가 1개뿐일 때도 `first`/`last`를 항상 넣고 싶다면 `pagedResourcesAssembler.setForceFirstAndLastRels(true)`를 사용할 수 있습니다.
  단, 이 Bean은 공유되므로 컨트롤러에서 매 요청 바꾸지 말고 설정 클래스에서 한 번만 지정해야 합니다. (이 문서에서는 기본 동작을 사용)

### 6.5 `page` 메타데이터

```json
"page": { "size": 10, "totalElements": 101, "totalPages": 11, "number": 3 }
```

spring-data-commons 3.4.2의 `PagedResourcesAssembler.asPageMetadata()`는 다음과 같이 동작합니다.

```java
int number = pageableResolver.isOneIndexedParameters() ? page.getNumber() + 1 : page.getNumber();
```

→ `one-indexed-parameters: true`이므로 **`number`도 1-based** 입니다. (`?page=3` → `"number": 3`)

### 6.6 빈 페이지 처리

`toModel()`에 빈 `Page`를 넘기면 `_embedded` 자체가 빠져 클라이언트가 `undefined`를 처리해야 합니다.
`toEmptyModel(page, ArticleResponse.class)`를 사용하면 `@Relation` 이름 그대로 빈 배열이 내려갑니다.

```json
{
  "_embedded": { "articles": [] },
  "_links": { "self": { "href": "...?boardId=999&page=1&size=10" } },
  "page": { "size": 10, "totalElements": 0, "totalPages": 0, "number": 1 }
}
```

---

## 7. 무한 스크롤(Cursor) 처리 상세

### 7.1 왜 `PagedModel`을 쓰지 않는가

| 항목 | 페이지 번호 방식 | Cursor 방식 |
|------|-------------|-------------|
| 위치 표현 | page 번호 (offset) | 마지막으로 본 `articleId` |
| 전체 개수/페이지 수 | 있음 (블록 제한) | 없음 |
| `prev` / `last` | 가능 | 불가능 (역방향 cursor 쿼리 없음) |
| 적합한 모델 | `PagedModel` | `CollectionModel` |

`SlicedResourcesAssembler`(Spring Data 3.1+)도 `Pageable`(page 번호) 기반이라 `lastArticleId` 방식과 맞지 않습니다.

### 7.2 `next` 링크 판단 규칙

```java
if (!articles.isEmpty() && articles.size() == pageSize) {
    Long nextCursor = articles.get(articles.size() - 1).getArticleId();
    links.add(... infinityScrollArticles(boardId, pageSize, nextCursor) ... NEXT);
}
```

- 쿼리가 `ORDER BY article_id DESC LIMIT :limit`이므로 **마지막 항목의 `articleId`가 다음 요청의 cursor** 입니다.
- 결과가 `size`보다 적으면 더 이상 데이터가 없으므로 `next`를 만들지 않습니다.
- 결과가 정확히 `size`개로 끝나는 경우 `next`가 생기고, 다음 요청에서 빈 목록이 반환됩니다. (쿼리를 바꾸지 않는 선에서 감수하는 1회 추가 요청)
  - 이를 없애려면 `limit = size + 1`로 조회해 초과분 존재 여부로 판단하는 방식이 있지만, Service 변경이 필요하므로 이 문서 범위에서는 제외했습니다.

### 7.3 링크 구성

| 링크 | 의미 | 예시 |
|------|------|------|
| `self` | 현재 요청 | `/api/v2/articles/infinity-scroll?boardId=1&size=10&lastArticleId=500` |
| `first` | 처음부터 (cursor 없음) | `/api/v2/articles/infinity-scroll?boardId=1&size=10` |
| `next` | 다음 묶음 | `/api/v2/articles/infinity-scroll?boardId=1&size=10&lastArticleId=490` |

`required = false`인 `lastArticleId`에 `null`을 넘기면 `methodOn`은 파라미터를 생략하지 않고 `...&size=10{&lastArticleId}` 처럼 **URI 템플릿 변수**를 남깁니다 (HAL에서 `"templated": true`).
그래서 `self`/`first` 링크는 `.expand()`를 호출해 값이 없는 선택 변수를 제거합니다. (`docs/hateoas` 프로젝트의 WebMvc 테스트로 확인)

---

## 8. API 응답 예시

> Content-Type: `application/hal+json`

### 8.1 페이지 목록 `GET /api/v2/articles?boardId=1&page=3&size=2`

```json
{
  "_embedded": {
    "articles": [
      {
        "articleId": 1205,
        "title": "title 1205",
        "content": "content 1205",
        "boardId": 1,
        "writerId": 1,
        "createdAt": "2026-10-05T10:00:00",
        "modifiedAt": "2026-10-05T10:00:00",
        "_links": {
          "self": { "href": "http://localhost:9000/api/v2/articles/1205" },
          "articles": { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=1&size=10" },
          "board-article-count": { "href": "http://localhost:9000/api/v2/articles/boards/1/article-count" }
        }
      },
      { "articleId": 1204, "...": "...", "_links": { "...": "..." } }
    ]
  },
  "_links": {
    "first": { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=1&size=2" },
    "prev":  { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=2&size=2" },
    "self":  { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=3&size=2" },
    "next":  { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=4&size=2" },
    "last":  { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=11&size=2" }
  },
  "page": { "size": 2, "totalElements": 21, "totalPages": 11, "number": 3 }
}
```

(size=2 → limit = `((3-1)/10 + 1) * 2 * 10 + 1 = 21`)

### 8.2 무한 스크롤 `GET /api/v2/articles/infinity-scroll?boardId=1&size=2`

```json
{
  "_embedded": {
    "articles": [
      { "articleId": 1210, "...": "...", "_links": { "self": { "href": "http://localhost:9000/api/v2/articles/1210" } } },
      { "articleId": 1209, "...": "...", "_links": { "self": { "href": "http://localhost:9000/api/v2/articles/1209" } } }
    ]
  },
  "_links": {
    "self":  { "href": "http://localhost:9000/api/v2/articles/infinity-scroll?boardId=1&size=2" },
    "first": { "href": "http://localhost:9000/api/v2/articles/infinity-scroll?boardId=1&size=2" },
    "next":  { "href": "http://localhost:9000/api/v2/articles/infinity-scroll?boardId=1&size=2&lastArticleId=1209" }
  }
}
```

### 8.3 단건 `GET /api/v2/articles/1205`

```json
{
  "articleId": 1205,
  "title": "title 1205",
  "content": "content 1205",
  "boardId": 1,
  "writerId": 1,
  "createdAt": "2026-10-05T10:00:00",
  "modifiedAt": "2026-10-05T10:00:00",
  "_links": {
    "self": { "href": "http://localhost:9000/api/v2/articles/1205" },
    "articles": { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=1&size=10" },
    "board-article-count": { "href": "http://localhost:9000/api/v2/articles/boards/1/article-count" }
  }
}
```

### 8.4 게시글 수 `GET /api/v2/articles/boards/1/article-count`

```json
{
  "boardId": 1,
  "articleCount": 1000,
  "_links": {
    "self": { "href": "http://localhost:9000/api/v2/articles/boards/1/article-count" },
    "articles": { "href": "http://localhost:9000/api/v2/articles?boardId=1&page=1&size=10" }
  }
}
```

### 8.5 생성 `POST /api/v2/articles`

```
HTTP/1.1 201 Created
Location: http://localhost:9000/api/v2/articles/1211
Content-Type: application/hal+json
```

본문은 8.3과 같은 형식입니다.

### 8.6 삭제 `DELETE /api/v2/articles/1211`

```
HTTP/1.1 204 No Content
```

---

## 9. 테스트 코드

### 9.1 `service/article/src/test/java/kr/co/won/article/controller/ArticleHateoasControllerTest.java` (신규)

기존 `ArticleControllerTest`와 같이 **실행 중인 서버(9000)** 에 `RestClient`로 요청하는 방식입니다.
HAL 응답은 `JsonNode`로 받아 JSON Pointer(`/_links/next/href`)로 검증합니다.

```java
package kr.co.won.article.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class ArticleHateoasControllerTest {

    RestClient restClient = RestClient.create("http://127.0.0.1:9000");

    @DisplayName(value = "01. create article -> 201 Created + Location + self link")
    @Test
    void createTests() {
        ResponseEntity<JsonNode> response = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(response.getHeaders().getLocation().toString(), response.getBody().at("/_links/self/href").asText());
        assertTrue(MediaType.parseMediaType("application/hal+json").isCompatibleWith(response.getHeaders().getContentType()));
    }

    @DisplayName(value = "02. read article -> self / articles / board-article-count links")
    @Test
    void readTests() {
        JsonNode created = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L)).getBody();
        String selfHref = created.at("/_links/self/href").asText();

        // 링크를 그대로 따라간다 (URL 하드코딩 X)
        JsonNode article = restClient.get().uri(selfHref).retrieve().body(JsonNode.class);

        assertEquals(created.get("articleId").asLong(), article.get("articleId").asLong());
        assertTrue(article.at("/_links/articles/href").asText().contains("boardId=1&page=1&size=10"));
        assertTrue(article.at("/_links/board-article-count/href").asText().endsWith("/api/v2/articles/boards/1/article-count"));
    }

    @DisplayName(value = "03. paging -> 1-based page, first/prev/self/next/last links")
    @Test
    void pagingTests() {
        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v2/articles")
                        .queryParam("boardId", 1L)
                        .queryParam("page", 2L)
                        .queryParam("size", 10L)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        assertEquals(10, body.at("/_embedded/articles").size());
        assertEquals(2, body.at("/page/number").asInt());          // one-indexed 메타데이터
        assertEquals(10, body.at("/page/size").asInt());
        assertTrue(body.at("/_links/self/href").asText().contains("page=2"));
        assertTrue(body.at("/_links/prev/href").asText().contains("page=1"));
        assertTrue(body.at("/_links/next/href").asText().contains("page=3"));
        assertTrue(body.at("/_links/first/href").asText().contains("page=1"));
        assertTrue(body.at("/_links/self/href").asText().contains("boardId=1")); // 다른 파라미터 유지
    }

    @DisplayName(value = "04. paging -> next 링크를 따라가면 다음 페이지")
    @Test
    void pagingFollowNextLinkTests() {
        JsonNode first = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=10").retrieve().body(JsonNode.class);
        JsonNode second = restClient.get().uri(first.at("/_links/next/href").asText()).retrieve().body(JsonNode.class);

        assertEquals(2, second.at("/page/number").asInt());
        long firstLastId = first.at("/_embedded/articles").get(9).get("articleId").asLong();
        long secondFirstId = second.at("/_embedded/articles").get(0).get("articleId").asLong();
        assertTrue(firstLastId > secondFirstId); // article_id DESC 정렬 유지
    }

    @DisplayName(value = "05. paging -> 블록 제한 count (page 1~10 은 totalElements <= size * 10 + 1)")
    @Test
    void pagingBlockLimitTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=10").retrieve().body(JsonNode.class);
        assertTrue(body.at("/page/totalElements").asLong() <= 101L);
    }

    @DisplayName(value = "06. paging -> max-page-size 제한")
    @Test
    void pagingMaxSizeTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=1&page=1&size=1000").retrieve().body(JsonNode.class);
        assertEquals(50, body.at("/page/size").asInt());
    }

    @DisplayName(value = "07. paging -> 빈 게시판은 _embedded.articles 빈 배열")
    @Test
    void pagingEmptyTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles?boardId=999999&page=1&size=10").retrieve().body(JsonNode.class);
        assertTrue(body.at("/_embedded/articles").isArray());
        assertEquals(0, body.at("/_embedded/articles").size());
        assertTrue(body.at("/_links/next").isMissingNode());
    }

    @DisplayName(value = "08. infinity scroll -> next 링크의 lastArticleId 는 마지막 항목 ID")
    @Test
    void infinityScrollTests() {
        JsonNode first = restClient.get().uri("/api/v2/articles/infinity-scroll?boardId=1&size=10").retrieve().body(JsonNode.class);

        JsonNode articles = first.at("/_embedded/articles");
        long lastArticleId = articles.get(articles.size() - 1).get("articleId").asLong();
        String nextHref = first.at("/_links/next/href").asText();
        assertTrue(nextHref.contains("lastArticleId=" + lastArticleId));

        JsonNode second = restClient.get().uri(nextHref).retrieve().body(JsonNode.class);
        assertTrue(second.at("/_embedded/articles").get(0).get("articleId").asLong() < lastArticleId);
    }

    @DisplayName(value = "09. board article count -> DTO + links")
    @Test
    void boardCountTests() {
        JsonNode body = restClient.get().uri("/api/v2/articles/boards/{boardId}/article-count", 1L).retrieve().body(JsonNode.class);
        assertEquals(1L, body.get("boardId").asLong());
        assertTrue(body.has("articleCount"));
        assertTrue(body.at("/_links/articles/href").asText().contains("boardId=1"));
    }

    @DisplayName(value = "10. update / delete")
    @Test
    void updateAndDeleteTests() {
        JsonNode created = createArticle(new ArticleCreateRequest("hi", "my content", 1L, 1L)).getBody();
        String selfHref = created.at("/_links/self/href").asText();

        JsonNode updated = restClient.put().uri(selfHref)
                .body(new ArticleUpdateRequest("update", "testing"))
                .retrieve()
                .body(JsonNode.class);
        assertEquals("update", updated.get("title").asText());

        ResponseEntity<Void> deleted = restClient.delete().uri(selfHref).retrieve().toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    ResponseEntity<JsonNode> createArticle(ArticleCreateRequest request) {
        return restClient.post()
                .uri("/api/v2/articles")
                .body(request)
                .retrieve()
                .toEntity(JsonNode.class);
    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class ArticleCreateRequest {
        private String title;
        private String content;
        private Long writerId;
        private Long boardId;
    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class ArticleUpdateRequest {
        private String title;
        private String content;
    }
}
```

**설명**

- `MappingJackson2HttpMessageConverter`는 `application/*+json`을 지원하므로 `application/hal+json` 응답을 `JsonNode`로 받을 수 있습니다.
- 테스트 02, 04, 08, 10은 **URL을 직접 만들지 않고 응답의 링크를 따라가는** HATEOAS 사용 방식 자체를 검증합니다.
- `PagedModel<EntityModel<ArticleResponse>>`로 직접 역직렬화하려면 RestClient에 HAL 지원 설정이 필요합니다.

  ```java
  // Spring Boot 환경(빈 주입 가능)에서
  RestClient restClient = RestClient.builder()
          .apply(hypermediaRestClientConfigurer::registerHypermediaTypes) // HypermediaRestClientConfigurer
          .baseUrl("http://127.0.0.1:9000")
          .build();

  PagedModel<EntityModel<ArticleResponse>> page = restClient.get()
          .uri("/api/v2/articles?boardId=1&page=1&size=10")
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
  ```

### 9.2 기존 테스트

- `ArticleControllerTest` (v1): **수정 없음**. v1 응답 형식이 그대로이므로 계속 통과해야 합니다. → 회귀 테스트 역할
- `PageLimitCalculatorTest`, `ArticleRepositoryTest`: **수정 없음**

---

## 10. 주의 사항 / 함정

| # | 항목 | 내용 | 대응 |
|---|------|------|------|
| 1 | `last` 링크 의미 | 블록 제한 count 때문에 큰 게시판에서는 "다음 블록 첫 페이지" | 6.3 내용을 API 문서/프런트에 공유 |
| 2 | `PageImpl` total 보정 | 마지막 페이지에서 total이 `offset + content.size()`로 바뀜 | 의도된 동작. 테스트 05로 범위 확인 |
| 3 | 1-based 일관성 | `one-indexed-parameters` 누락 시 `page=1`이 두 번째 페이지가 되고, 링크도 0-based로 생성됨 | yml 설정 필수 + 테스트 03 |
| 4 | `methodOn` 반환 타입 | 링크 대상 메서드가 `Long` 같은 `final` 타입을 반환하면 프록시 생성 불가 | v2는 모두 `EntityModel`/`PagedModel`/`CollectionModel`/`ResponseEntity` 반환 |
| 5 | Snowflake ID와 JavaScript | `articleId`가 2^53보다 커서 JS `Number`에서 정밀도 손실 가능 (v1과 동일한 기존 이슈) | 필요 시 `@JsonSerialize(using = ToStringSerializer.class)` 별도 검토 |
| 6 | 프록시/게이트웨이 | 링크 host가 `localhost:9000`으로 나오면 외부에서 접근 불가 | Gateway 뒤에 둘 경우 `server.forward-headers-strategy: framework` 설정으로 `X-Forwarded-*` 반영 |
| 7 | 존재하지 않는 게시글 | `findArticle`의 `orElseThrow()` → `NoSuchElementException` → 500 (v1과 동일) | 별도 과제: `@RestControllerAdvice`로 404 + `application/problem+json` |
| 8 | 내부 서비스 | `article-read`, `hot-article`은 계속 v1 사용 | v1 삭제 시 부록 A 순서대로 마이그레이션 |

---

## 부록 A. v1을 직접 HATEOAS로 바꿀 경우 수정해야 하는 클라이언트 코드

나중에 v1을 없애고 `/api/articles` 자체를 HAL로 바꾸려면, 아래 클라이언트를 **먼저** 수정·배포한 뒤 Article 서비스를 배포해야 합니다.

### A.1 `service/article-read/.../client/ArticleClient.java`

| 메서드 | 현재 | 변경 |
|--------|------|------|
| `readArticle` | `body(ArticleResponse.class)` | 변경 불필요 (`RestClient.create()`의 Jackson 컨버터는 `Jackson2ObjectMapperBuilder` 기본값인 `FAIL_ON_UNKNOWN_PROPERTIES=false`를 사용하므로 `_links`는 무시됨) |
| `pagingArticleListResponse` | `body(ArticlePageResponse.class)` → `articles`, `articleCount` | `_embedded.articles`, `page.totalElements`로 매핑 필요 |
| `infinityScrollArticleListResponse` | `List<ArticleResponse>` | `_embedded.articles`로 매핑 필요 |
| `countArtice` | `Long` | `articleCount` 필드에서 추출 |

```java
// 예시: Paging 응답을 HAL 구조로 받기 위한 DTO
@Getter
@NoArgsConstructor
static class HalArticlePageResponse {
    @JsonProperty("_embedded")
    private Embedded embedded;
    private PageMetadata page;

    @Getter
    @NoArgsConstructor
    static class Embedded {
        private List<ArticleResponse> articles = List.of();
    }

    @Getter
    @NoArgsConstructor
    static class PageMetadata {
        private long size;
        private long totalElements;
        private long totalPages;
        private long number;
    }

    public ArticlePageResponse toArticlePageResponse() {
        List<ArticleResponse> articles = embedded == null ? List.of() : embedded.getArticles();
        return new ArticlePageResponse(articles, page == null ? 0L : page.getTotalElements());
    }
}
```

### A.2 `service/hot-article/.../client/ArticleClient.java`

- `readArticle`만 사용 → 단건 응답에 `_links`가 추가될 뿐이라 **변경 불필요**합니다. (알 수 없는 필드 무시)

### A.3 테스트 코드

- `service/article/.../ArticleControllerTest`: paging(05), infinity scroll(06), count(07) 테스트를 HAL 구조로 변경
- `service/article-read/.../ArticleReadApiTests`: `/api/articles` 원본 비교 부분을 HAL 구조로 변경
- `DataInitializer`(article-read, hot-article): POST 응답 body를 쓰지 않으면 변경 불필요

---

## 부록 B. 확장 아이디어

1. **HAL-FORMS Affordance**: 단건 응답에 "수정/삭제 가능" 정보를 메서드 포함해서 노출
   ```java
   linkTo(methodOn(ArticleHateoasController.class).readArticle(id)).withSelfRel()
           .andAffordance(afford(methodOn(ArticleHateoasController.class).updateArticle(id, null)))
           .andAffordance(afford(methodOn(ArticleHateoasController.class).deleteArticle(id)));
   ```
   → `@EnableHypermediaSupport(type = { HAL, HAL_FORMS })` 와 `Accept: application/prs.hal-forms+json` 필요
2. **Problem Details**: 예외를 `application/problem+json` (RFC 9457)으로 통일 (`spring.mvc.problemdetails.enabled: true`)
3. **API 문서화**: Spring REST Docs의 `links(...)` 스니펫으로 링크 명세를 테스트 기반 문서화
4. **Cursor 정밀화**: Repository에서 `limit = size + 1`로 조회해 `next` 존재 여부를 추가 요청 없이 정확히 판단
