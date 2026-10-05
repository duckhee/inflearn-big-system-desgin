# HATEOAS 마이그레이션 프로젝트 (article 서비스)

[`../article-service-hateoas-guide.md`](../article-service-hateoas-guide.md) 문서의 내용을 **실제로 빌드하고 테스트할 수 있는 코드**로 옮긴 독립 Gradle 프로젝트입니다.

- 원본 프로젝트(`service/article`)는 **전혀 수정하지 않았습니다.**
- 공통 모듈(`common/*`)은 복사하지 않고 원본 디렉토리를 그대로 참조합니다.
- 기존 v1 API(`/api/articles`)는 그대로 두고, HATEOAS API(`/api/v2/articles`)를 추가하는 방식입니다.

---

## 1. 실행 방법

루트 프로젝트 디렉토리(`inflearn-big-system-design`)에서 실행합니다. Gradle Wrapper는 루트의 것을 그대로 씁니다.

```bash
# 컴파일
./gradlew -p docs/hateoas :service:article:compileJava

# DB 없이 실행 가능한 테스트 (HATEOAS 링크/페이징 검증 13개 + PageLimitCalculator)
./gradlew -p docs/hateoas :service:article:test \
  --tests '*ArticleHateoasControllerWebMvcTest' \
  --tests '*PageLimitCalculatorTest'

# 애플리케이션 실행 (원본과 동일하게 .env 에 MySQL / Redis / Kafka 접속 정보 필요, port 9000)
./gradlew -p docs/hateoas :service:article:bootRun
```

> ⚠️ 원본 `service/article`과 포트(9000)가 같습니다. 둘 중 하나만 실행하세요.
> ⚠️ 공통 모듈의 빌드 결과물(`common/*/build`)은 원본 빌드와 같은 위치를 공유합니다. 출력 내용이 같으므로 동작에는 문제가 없습니다.

---

## 2. 디렉토리 구조

```
docs/hateoas
├── README.md                          ← 이 문서
├── settings.gradle                    ← 독립 빌드 설정 (common:* 는 ../../common/* 참조)
├── build.gradle                       ← 원본 루트 build.gradle 의 allprojects 설정과 동일
└── service/article
    ├── build.gradle                   ← [변경] spring-boot-starter-hateoas 추가
    └── src
        ├── main
        │   ├── java/kr/co/won/article
        │   │   ├── ArticleApplication.java              ← [변경] JPA 설정을 JpaConfiguration 으로 분리
        │   │   ├── configuration/JpaConfiguration.java  ← [신규] @EntityScan / @EnableJpaRepositories
        │   │   ├── controller
        │   │   │   ├── ArticleController.java           ← [유지] v1 /api/articles (내부 서비스용)
        │   │   │   ├── ArticleHateoasController.java    ← [신규] v2 /api/v2/articles (HATEOAS)
        │   │   │   └── hateoas/ArticleModelAssembler.java ← [신규] ArticleResponse → EntityModel + 링크
        │   │   ├── entity/*                             ← [유지]
        │   │   ├── repository/*                         ← [유지] 쿼리 변경 없음
        │   │   └── service
        │   │       ├── ArticleService.java              ← [유지] (사용하지 않던 중복 필드 1개만 제거)
        │   │       ├── request/*                        ← [유지]
        │   │       ├── response/ArticleResponse.java    ← [변경] @Relation 추가
        │   │       ├── response/ArticlePageResponse.java← [유지]
        │   │       ├── response/BoardArticleCountResponse.java ← [신규] Long → DTO
        │   │       └── utils/paging/PageLimitCalculator.java   ← [유지]
        │   └── resources
        │       ├── application.yml            ← [변경] spring.data.web.pageable.* 추가
        │       └── META-INF/orm.xml, db/, logback-dev.xml, application-db.yml ← [유지]
        └── test
            ├── java/kr/co/won/article
            │   ├── controller/ArticleHateoasControllerWebMvcTest.java ← [신규] DB 없이 실행 (Service Mock)
            │   ├── controller/ArticleHateoasControllerTest.java       ← [신규] 실행 중인 서버 대상 통합 테스트
            │   ├── controller/ArticleControllerTest.java              ← [유지] v1 회귀 테스트
            │   ├── repository/ArticleRepositoryTest.java              ← [유지] MySQL 필요
            │   ├── data/DataInitializer.java                          ← [유지] MySQL 필요
            │   └── service/utils/paging/PageLimitCalculatorTest.java  ← [유지]
            └── resources/application.yml      ← [변경] pageable 설정 추가
```

각 파일 맨 위 주석에 `[원본 코드 유지]`, `[HATEOAS 변경]`, `[HATEOAS 신규]` 표시와 이유를 달아 두었습니다.

---

## 3. API 비교 (v1 → v2)

| 기능 | v1 (유지) | v2 (HATEOAS) | v2 응답 타입 |
|------|-----------|--------------|--------------|
| 페이지 목록 | `GET /api/articles?boardId&page&size` | `GET /api/v2/articles?boardId&page&size` | `PagedModel<EntityModel<ArticleResponse>>` |
| 무한 스크롤 | `GET /api/articles/infinity-scroll` | `GET /api/v2/articles/infinity-scroll` | `CollectionModel<EntityModel<ArticleResponse>>` |
| 단건 조회 | `GET /api/articles/{id}` | `GET /api/v2/articles/{id}` | `EntityModel<ArticleResponse>` |
| 게시글 수 | `GET /api/articles/boards/{boardId}/article-count` → `Long` | `GET /api/v2/articles/boards/{boardId}/article-count` | `EntityModel<BoardArticleCountResponse>` |
| 생성 | `POST /api/articles` → 200 | `POST /api/v2/articles` → **201 + Location** | `EntityModel<ArticleResponse>` |
| 수정 | `PUT /api/articles/{id}` | `PUT /api/v2/articles/{id}` | `EntityModel<ArticleResponse>` |
| 삭제 | `DELETE /api/articles/{id}` → 200 | `DELETE /api/v2/articles/{id}` → **204** | 본문 없음 |

---

## 4. 페이징 처리 요약

```
GET /api/v2/articles?boardId=1&page=3&size=10
  │
  ├─ Pageable 해석 (one-indexed-parameters: true)   → pageNumber=2(0-based), size=10 (최대 50)
  ├─ ArticleService.pageArticle(1, 2+1, 10)          → 기존 v1 과 같은 호출
  │     └─ PageLimitCalculator → count limit = 101 (블록 제한 count)
  ├─ new PageImpl<>(articles, pageable, 101)         → Spring Data Page 로 감싸기
  └─ PagedResourcesAssembler.toModel(page, ArticleModelAssembler)
        ├─ _embedded.articles[*]._links.self
        ├─ _links: first(page=1) prev(page=2) self(page=3) next(page=4) last(page=11)
        └─ page: { size:10, totalElements:101, totalPages:11, number:3 }
```

| 규칙 | 내용 |
|------|------|
| 페이지 번호 | 요청, 링크, `page.number` 모두 1부터 시작 |
| `last` 링크 | 블록 제한 count 때문에 데이터가 많으면 "다음 블록의 첫 페이지" (page 1~10 → 11) |
| `boardId` | 링크의 기준 URL이 현재 요청 URL이므로 자동 유지 |
| 빈 페이지 | `toEmptyModel` → `_embedded.articles: []` |
| 무한 스크롤 `next` | 결과 개수 == size 일 때만, 마지막 `articleId`를 `lastArticleId`로 사용 |

자세한 설명은 [가이드 문서 6장, 7장](../article-service-hateoas-guide.md#6-paging-처리-상세)에 있습니다.

---

## 5. 테스트로 확인한 내용 (`ArticleHateoasControllerWebMvcTest`, 13개 통과)

| # | 검증 내용 |
|---|-----------|
| 01 | 첫 페이지: `prev` 없음, `first`/`next`/`last`(=11) 생성, `page.number`=1, `boardId` 유지, Content-Type `application/hal+json` |
| 02 | 중간 페이지: Service에 1-based page(3) 그대로 전달, `prev`/`next` 생성 |
| 03 | 마지막 페이지: total 35 → totalPages 4, `next` 없음 |
| 04 | page/size 생략 → size 10, page 1 |
| 05 | `size=1000` → 50으로 제한되어 Service 호출 |
| 06 | 빈 게시판 → `_embedded.articles` 빈 배열 |
| 07 | 무한 스크롤 가득 참 → `next`의 `lastArticleId` = 마지막 항목 ID |
| 08 | 무한 스크롤 마지막 묶음 → `next` 없음 |
| 09 | 단건 → `self` / `articles` / `board-article-count` 링크 |
| 10 | 게시글 수 → DTO + 링크 |
| 11 | 생성 → 201 + `Location` = `self` |
| 12 | 수정 → `self` 링크 |
| 13 | 삭제 → 204 |

### 테스트 중 발견해서 반영한 2가지

1. **null 선택 파라미터가 템플릿으로 남음**
   `methodOn(...).infinityScrollArticles(boardId, size, null)` → `...&size=3{&lastArticleId}`
   → `self`/`first` 링크에 `.expand()`를 호출해 제거했습니다. (가이드 문서도 수정)
2. **MockMvc `.param()`은 query string을 채우지 않음**
   `PagedResourcesAssembler`는 `request.getQueryString()`을 기준 URL로 쓰기 때문에 `.param("boardId", ...)`로 요청하면 링크에서 `boardId`가 빠집니다.
   → 테스트는 `get("/api/v2/articles?boardId=1&page=1&size=10")`처럼 URI에 직접 씁니다. (실제 서버에서는 문제 없음)

---

## 6. 원본에 반영하는 방법

이 프로젝트의 `service/article` 아래 파일을 원본 `service/article`의 같은 경로로 옮기면 됩니다.

1. `service/article/build.gradle`에 `spring-boot-starter-hateoas` 추가
2. `src/main/resources/application.yml`, `src/test/resources/application.yml`에 `spring.data.web.pageable` 블록 추가
3. 신규 파일 복사: `configuration/JpaConfiguration.java`, `controller/ArticleHateoasController.java`, `controller/hateoas/*`, `service/response/BoardArticleCountResponse.java`, 테스트 2개
4. 변경 파일 반영: `ArticleApplication.java`(JPA 어노테이션 제거), `ArticleResponse.java`(`@Relation`)
5. `article-read`, `hot-article`은 v1을 계속 쓰므로 **수정할 필요가 없습니다.**
