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
 * 게시글 API v2 - HATEOAS(HAL, application/hal+json) [HATEOAS 신규]
 *
 * [설계]
 * - v1({@link ArticleController})은 내부 서비스가 사용하므로 그대로 두고, 외부 클라이언트용으로 /api/v2/articles 를 제공한다.
 * - Service 는 v1 과 같은 메서드를 그대로 호출한다. 이 컨트롤러는 "응답에 링크를 붙이는 일"만 한다.
 *
 * [반환 타입 규칙]
 * - 링크 대상이 되는 메서드는 EntityModel / PagedModel / CollectionModel / ResponseEntity 처럼 프록시 가능한(non-final) 타입을 반환한다.
 *   => methodOn(...) 은 메서드 반환 타입의 프록시를 만들어 호출을 기록하므로, Long 같은 final 타입을 반환하면 링크를 만들 수 없다.
 */
@RestController
@RequestMapping(path = "/api/v2/articles")
@RequiredArgsConstructor
public class ArticleHateoasController {

    private final ArticleService articleService;
    private final ArticleModelAssembler articleModelAssembler;
    /**
     * spring-data-commons 가 제공하는 페이지 -> PagedModel 변환기
     * => spring-boot-starter-hateoas + spring-data 가 있으면 Boot 자동 설정으로 Bean 이 등록된다.
     * => 제네릭 타입과 무관하게 같은 Bean 하나가 주입된다.
     */
    private final PagedResourcesAssembler<ArticleResponse> pagedResourcesAssembler;

    /**
     * 페이지 번호 방식 목록 조회
     * ex) GET /api/v2/articles?boardId=1&page=1&size=10
     *
     * [처리 순서]
     * 1. Pageable 수신 : one-indexed-parameters=true 이므로 ?page=1 -> pageable.getPageNumber() == 0
     *                    size 미지정 시 10(default-page-size), 50 초과 시 50(max-page-size)
     * 2. Service 호출  : 기존 Service 는 1-based 이므로 +1 해서 넘긴다. (v1 과 동일한 값이 넘어간다)
     * 3. PageImpl 래핑 : (목록, pageable, 블록 제한 count) -> Spring Data Page
     * 4. PagedModel 변환 : first / prev / self / next / last 링크 + page 메타데이터 자동 생성
     *
     * @param boardId  게시판 ID (Pageable 이 아닌 파라미터지만, 생성되는 페이지 링크에 자동으로 유지된다)
     * @param pageable 페이지 정보
     */
    @GetMapping
    public PagedModel<EntityModel<ArticleResponse>> pageArticles(@RequestParam("boardId") Long boardId, Pageable pageable) {
        // 2. Pageable(0-based) -> Service(1-based)
        long pageNumber = pageable.getPageNumber() + 1L;
        long pageSize = pageable.getPageSize();
        ArticlePageResponse response = articleService.pageArticle(boardId, pageNumber, pageSize);

        /*
         * 3. PageImpl 로 감싼다.
         * - total 에 블록 제한 count 를 넣으므로 totalPages 도 "현재 블록 + 다음 블록 1페이지" 기준이 된다.
         *   ex) 게시글 1000건, size=10, page=3 -> count=101 -> totalPages=11 -> last 링크 = page=11
         * - PageImpl 은 (내용이 있고 offset + size > total) 이면 total 을 offset + 실제 개수로 보정한다.
         *   => 마지막 페이지에서는 실제 개수로 맞춰진다.
         */
        Page<ArticleResponse> articlePage = new PageImpl<>(response.getArticles(), pageable, response.getArticleCount());

        /*
         * 데이터가 없을 때 toModel() 을 쓰면 "_embedded" 자체가 빠진다.
         * toEmptyModel(page, 타입) 은 @Relation 이름으로 빈 배열("_embedded.articles": [])을 내려준다.
         */
        if (articlePage.isEmpty()) {
            @SuppressWarnings("unchecked")
            PagedModel<EntityModel<ArticleResponse>> emptyModel =
                    (PagedModel<EntityModel<ArticleResponse>>) pagedResourcesAssembler.toEmptyModel(articlePage, ArticleResponse.class);
            return emptyModel;
        }

        /*
         * 4. PagedModel 변환
         * - 링크 기준 URL 은 현재 요청 URL(ServletUriComponentsBuilder.fromCurrentRequest)이라 boardId 는 유지되고 page/size 만 교체된다.
         * - first/last : 페이지가 2개 이상일 때, prev : 이전 페이지가 있을 때, next : 다음 페이지가 있을 때만 생성된다.
         * - 각 항목은 articleModelAssembler 로 변환되어 self 등 링크가 붙는다.
         */
        return pagedResourcesAssembler.toModel(articlePage, articleModelAssembler);
    }

    /**
     * 무한 스크롤(Cursor) 방식 목록 조회
     * ex) GET /api/v2/articles/infinity-scroll?boardId=1&size=10
     * ex) GET /api/v2/articles/infinity-scroll?boardId=1&size=10&lastArticleId=123
     *
     * [PagedModel 을 쓰지 않는 이유]
     * - cursor 방식은 page 번호 / 전체 개수 개념이 없어 Page 로 표현할 수 없다.
     *   (SlicedResourcesAssembler 도 page 번호 기반이라 lastArticleId 방식과 맞지 않는다)
     * - 그래서 CollectionModel 에 self / first / next 링크를 직접 붙인다.
     */
    @GetMapping(path = "/infinity-scroll")
    public CollectionModel<EntityModel<ArticleResponse>> infinityScrollArticles(@RequestParam("boardId") Long boardId,
                                                                                @RequestParam("size") Long pageSize,
                                                                                @RequestParam(value = "lastArticleId", required = false) Long lastArticleId) {
        List<ArticleResponse> articles = articleService.infinityScrollArticle(boardId, pageSize, lastArticleId);

        List<Link> links = new ArrayList<>();
        /*
         * self : 현재 요청 그대로
         * first : cursor 없이 처음부터
         * - required = false 인 파라미터에 null 을 넘기면 methodOn 은 값을 생략하지 않고
         *   "...&size=3{&lastArticleId}" 처럼 URI 템플릿 변수를 남긴다. (HAL 에서는 "templated": true 링크가 된다)
         * - expand() 를 호출하면 값이 없는 선택 변수가 제거되어 "...&size=3" 처럼 바로 따라갈 수 있는 링크가 된다.
         */
        links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, lastArticleId)).withSelfRel().expand());
        links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, null)).withRel(IanaLinkRelations.FIRST).expand());
        /*
         * next : 요청한 개수만큼 가득 찼을 때만 "다음 데이터가 있을 수 있다"고 판단한다.
         * - 쿼리가 ORDER BY article_id DESC 이므로 이번 결과의 마지막 articleId 가 다음 요청의 cursor 가 된다.
         * - 결과가 size 보다 적으면 마지막 묶음이므로 next 를 만들지 않는다.
         * - 정확히 size 개로 끝나는 경우 다음 요청에서 빈 목록이 한 번 반환된다. (쿼리를 바꾸지 않는 선에서 감수)
         */
        if (!articles.isEmpty() && articles.size() == pageSize) {
            Long nextCursor = articles.get(articles.size() - 1).getArticleId();
            links.add(linkTo(methodOn(ArticleHateoasController.class).infinityScrollArticles(boardId, pageSize, nextCursor)).withRel(IanaLinkRelations.NEXT));
        }

        // 각 항목은 Assembler 로 변환 (항목별 self 등 링크) 후 목록 자체의 링크를 추가
        return articleModelAssembler.toCollectionModel(articles).add(links);
    }

    /**
     * 게시글 단건 조회
     * ex) GET /api/v2/articles/{articleId}
     * => 응답 링크: self, articles(게시판 첫 페이지), board-article-count
     */
    @GetMapping(path = "/{articleId}")
    public EntityModel<ArticleResponse> readArticle(@PathVariable("articleId") Long articleId) {
        return articleModelAssembler.toModel(articleService.findArticle(articleId));
    }

    /**
     * 게시판 별 게시글 수 조회
     * ex) GET /api/v2/articles/boards/{boardId}/article-count
     * => v1 은 Long 을 반환하지만 링크를 붙이기 위해 BoardArticleCountResponse 로 감싼다.
     */
    @GetMapping(path = "/boards/{boardId}/article-count")
    public EntityModel<BoardArticleCountResponse> boardArticleCount(@PathVariable("boardId") Long boardId) {
        BoardArticleCountResponse response = BoardArticleCountResponse.of(boardId, articleService.articleCountNumber(boardId));
        return EntityModel.of(response,
                linkTo(methodOn(ArticleHateoasController.class).boardArticleCount(boardId)).withSelfRel(),
                ArticleModelAssembler.boardArticlesLink(boardId)
        );
    }

    /**
     * 게시글 생성
     * => REST 규약대로 201 Created + Location 헤더(생성된 리소스의 self 링크) + 본문(EntityModel)
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
     * => 수정된 게시글 + 링크
     */
    @PutMapping(path = "/{articleId}")
    public EntityModel<ArticleResponse> updateArticle(@PathVariable("articleId") Long articleId, @RequestBody ArticleUpdateRequest request) {
        return articleModelAssembler.toModel(articleService.updateArticle(articleId, request));
    }

    /**
     * 게시글 삭제
     * => 204 No Content (삭제된 리소스에는 더 이상 따라갈 링크가 없으므로 본문 없음)
     */
    @DeleteMapping(path = "/{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("articleId") Long articleId) {
        articleService.deleteArticle(articleId);
        return ResponseEntity.noContent().build();
    }
}
