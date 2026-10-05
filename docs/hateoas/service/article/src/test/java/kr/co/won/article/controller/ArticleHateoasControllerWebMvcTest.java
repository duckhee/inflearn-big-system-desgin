package kr.co.won.article.controller;

import kr.co.won.article.controller.hateoas.ArticleModelAssembler;
import kr.co.won.article.entity.ArticleEntity;
import kr.co.won.article.service.ArticleService;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * v2(HATEOAS) 컨트롤러 슬라이스 테스트 [HATEOAS 신규]
 *
 * - DB / Kafka / Redis / 실행 중인 서버 없이 실행 가능하다.
 *   => @WebMvcTest 는 Web 계층(컨트롤러, Jackson, HATEOAS, Spring Data Web 자동 설정)만 로딩한다.
 *   => ArticleService 는 @MockitoBean 으로 대체하고, @Component 인 ArticleModelAssembler 는 @Import 로 직접 등록한다.
 * - application.yml 의 spring.data.web.pageable.* 설정(one-indexed, max-page-size 등)이 그대로 적용된다.
 * - MockMvc 요청의 기본 host 는 http://localhost 이므로 링크도 http://localhost/... 로 생성된다.
 * - 쿼리 파라미터는 .param(...) 이 아니라 URI 에 직접 쓴다.
 *   => PagedResourcesAssembler 는 현재 요청의 query string(request.getQueryString())을 기준 URL 로 사용하는데,
 *      MockMvc 의 .param(...) 은 query string 을 채우지 않아 boardId 가 링크에서 빠진 것처럼 보이기 때문이다.
 *      (실제 서버에서는 항상 query string 이 있으므로 boardId 가 유지된다)
 */
@WebMvcTest(controllers = ArticleHateoasController.class)
@Import(ArticleModelAssembler.class)
class ArticleHateoasControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ArticleService articleService;

    /** 테스트용 게시글 목록 생성 (articleId 내림차순: start, start-1, ...) */
    static List<ArticleResponse> articles(long boardId, long startId, int count) {
        return LongStream.range(0, count)
                .mapToObj(i -> ArticleResponse.fromEntity(ArticleEntity.createArticle(startId - i, "title" + (startId - i), "content", boardId, 1L)))
                .toList();
    }

    @DisplayName(value = "01. 첫 페이지 - prev 없음, next/first/last 생성, page.number 는 1-based, boardId 유지")
    @Test
    void firstPageTests() throws Exception {
        // 블록 제한 count = 101 (page 1~10, size 10)
        given(articleService.pageArticle(1L, 1L, 10L)).willReturn(ArticlePageResponse.of(articles(1L, 1000L, 10), 101L));

        mockMvc.perform(get("/api/v2/articles?boardId=1&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                // _embedded.articles : @Relation(collectionRelation = "articles")
                .andExpect(jsonPath("$._embedded.articles", hasSize(10)))
                .andExpect(jsonPath("$._embedded.articles[0].articleId").value(1000))
                .andExpect(jsonPath("$._embedded.articles[0]._links.self.href").value("http://localhost/api/v2/articles/1000"))
                // page 메타데이터 : one-indexed 이므로 number = 1, totalPages = ceil(101 / 10) = 11
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(101))
                .andExpect(jsonPath("$.page.totalPages").value(11))
                // 링크 : boardId 는 유지되고 page/size 만 바뀐다
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles?boardId=1&page=1&size=10"))
                .andExpect(jsonPath("$._links.first.href").value("http://localhost/api/v2/articles?boardId=1&page=1&size=10"))
                .andExpect(jsonPath("$._links.next.href").value("http://localhost/api/v2/articles?boardId=1&page=2&size=10"))
                // 블록 제한 count 때문에 last 는 "다음 블록의 첫 페이지"
                .andExpect(jsonPath("$._links.last.href").value("http://localhost/api/v2/articles?boardId=1&page=11&size=10"))
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }

    @DisplayName(value = "02. 중간 페이지 - Service 에는 1-based page 가 그대로 전달되고 prev/next 모두 생성")
    @Test
    void middlePageTests() throws Exception {
        given(articleService.pageArticle(1L, 3L, 10L)).willReturn(ArticlePageResponse.of(articles(1L, 980L, 10), 101L));

        mockMvc.perform(get("/api/v2/articles?boardId=1&page=3&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(3))
                .andExpect(jsonPath("$._links.prev.href").value("http://localhost/api/v2/articles?boardId=1&page=2&size=10"))
                .andExpect(jsonPath("$._links.next.href").value("http://localhost/api/v2/articles?boardId=1&page=4&size=10"));

        // v1 과 동일한 값(page=3)으로 Service 가 호출되었는지 확인
        verify(articleService).pageArticle(1L, 3L, 10L);
    }

    @DisplayName(value = "03. 마지막 페이지 - PageImpl total 보정, next 없음")
    @Test
    void lastPageTests() throws Exception {
        // 게시글 35건, page=4 -> 5건, count=35
        given(articleService.pageArticle(2L, 4L, 10L)).willReturn(ArticlePageResponse.of(articles(2L, 5L, 5), 35L));

        mockMvc.perform(get("/api/v2/articles?boardId=2&page=4&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.articles", hasSize(5)))
                .andExpect(jsonPath("$.page.totalElements").value(35))
                .andExpect(jsonPath("$.page.totalPages").value(4))
                .andExpect(jsonPath("$._links.prev.href").value(containsString("page=3")))
                .andExpect(jsonPath("$._links.last.href").value(containsString("page=4")))
                .andExpect(jsonPath("$._links.next").doesNotExist());
    }

    @DisplayName(value = "04. page/size 생략 - default-page-size(10), page 1")
    @Test
    void defaultPageableTests() throws Exception {
        given(articleService.pageArticle(1L, 1L, 10L)).willReturn(ArticlePageResponse.of(articles(1L, 1000L, 10), 101L));

        mockMvc.perform(get("/api/v2/articles?boardId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(1));

        verify(articleService).pageArticle(1L, 1L, 10L);
    }

    @DisplayName(value = "05. size 초과 - max-page-size(50) 로 제한되어 Service 에 전달")
    @Test
    void maxPageSizeTests() throws Exception {
        given(articleService.pageArticle(eq(1L), eq(1L), eq(50L))).willReturn(ArticlePageResponse.of(articles(1L, 1000L, 50), 501L));

        mockMvc.perform(get("/api/v2/articles?boardId=1&page=1&size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50));

        verify(articleService).pageArticle(1L, 1L, 50L);
    }

    @DisplayName(value = "06. 빈 게시판 - _embedded.articles 가 빈 배열로 내려간다")
    @Test
    void emptyPageTests() throws Exception {
        given(articleService.pageArticle(999L, 1L, 10L)).willReturn(ArticlePageResponse.of(List.of(), 0L));

        mockMvc.perform(get("/api/v2/articles?boardId=999&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.articles", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.next").doesNotExist());
    }

    @DisplayName(value = "07. 무한 스크롤 - 가득 찬 결과는 마지막 articleId 를 cursor 로 next 링크 생성")
    @Test
    void infinityScrollNextTests() throws Exception {
        given(articleService.infinityScrollArticle(1L, 3L, null)).willReturn(articles(1L, 100L, 3));

        mockMvc.perform(get("/api/v2/articles/infinity-scroll?boardId=1&size=3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.articles", hasSize(3)))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles/infinity-scroll?boardId=1&size=3"))
                .andExpect(jsonPath("$._links.first.href").value("http://localhost/api/v2/articles/infinity-scroll?boardId=1&size=3"))
                .andExpect(jsonPath("$._links.next.href").value("http://localhost/api/v2/articles/infinity-scroll?boardId=1&size=3&lastArticleId=98"));
    }

    @DisplayName(value = "08. 무한 스크롤 - size 보다 적게 오면 마지막 묶음이므로 next 없음")
    @Test
    void infinityScrollLastTests() throws Exception {
        given(articleService.infinityScrollArticle(1L, 3L, 98L)).willReturn(articles(1L, 2L, 2));

        mockMvc.perform(get("/api/v2/articles/infinity-scroll?boardId=1&size=3&lastArticleId=98"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.articles", hasSize(2)))
                .andExpect(jsonPath("$._links.self.href").value(containsString("lastArticleId=98")))
                .andExpect(jsonPath("$._links.next").doesNotExist());
    }

    @DisplayName(value = "09. 단건 조회 - self / articles / board-article-count 링크")
    @Test
    void readArticleTests() throws Exception {
        given(articleService.findArticle(1000L)).willReturn(articles(1L, 1000L, 1).get(0));

        mockMvc.perform(get("/api/v2/articles/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value(1000))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles/1000"))
                .andExpect(jsonPath("$._links.articles.href").value("http://localhost/api/v2/articles?boardId=1&page=1&size=10"))
                .andExpect(jsonPath("$._links.board-article-count.href").value("http://localhost/api/v2/articles/boards/1/article-count"));
    }

    @DisplayName(value = "10. 게시판 게시글 수 - Long 대신 DTO + 링크")
    @Test
    void boardArticleCountTests() throws Exception {
        given(articleService.articleCountNumber(1L)).willReturn(1000L);

        mockMvc.perform(get("/api/v2/articles/boards/1/article-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value(1))
                .andExpect(jsonPath("$.articleCount").value(1000))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles/boards/1/article-count"))
                .andExpect(jsonPath("$._links.articles.href").value(containsString("boardId=1")));
    }

    @DisplayName(value = "11. 생성 - 201 Created + Location 헤더 = self 링크")
    @Test
    void createArticleTests() throws Exception {
        given(articleService.createArticle(any())).willReturn(articles(1L, 2000L, 1).get(0));

        mockMvc.perform(post("/api/v2/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hi\",\"content\":\"my content\",\"writerId\":1,\"boardId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v2/articles/2000"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles/2000"));
    }

    @DisplayName(value = "12. 수정 - 수정 결과 + 링크")
    @Test
    void updateArticleTests() throws Exception {
        given(articleService.updateArticle(eq(1000L), any())).willReturn(articles(1L, 1000L, 1).get(0));

        mockMvc.perform(put("/api/v2/articles/1000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"update\",\"content\":\"testing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v2/articles/1000"));
    }

    @DisplayName(value = "13. 삭제 - 204 No Content")
    @Test
    void deleteArticleTests() throws Exception {
        mockMvc.perform(delete("/api/v2/articles/1000"))
                .andExpect(status().isNoContent());

        verify(articleService).deleteArticle(1000L);
    }
}
