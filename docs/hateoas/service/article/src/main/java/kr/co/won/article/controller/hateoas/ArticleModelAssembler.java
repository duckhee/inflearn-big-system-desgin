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
 * ArticleResponse -> EntityModel&lt;ArticleResponse&gt; 변환기 [HATEOAS 신규]
 *
 * - 단건 조회 / 생성 / 수정 응답, 그리고 목록(_embedded.articles)의 각 항목에 "같은 링크 규칙"을 적용하기 위해 한 곳에 모은다.
 * - RepresentationModelAssembler 를 구현하면
 *   PagedResourcesAssembler.toModel(page, 이 Assembler) 로 넘겨 페이지 목록의 각 항목 변환에도 그대로 재사용할 수 있다.
 *
 * 생성되는 링크
 *   self                : GET /api/v2/articles/{articleId}
 *   articles            : GET /api/v2/articles?boardId={boardId}&page=1&size=10  (이 게시글이 속한 게시판 첫 페이지)
 *   board-article-count : GET /api/v2/articles/boards/{boardId}/article-count
 */
@Component
public class ArticleModelAssembler implements RepresentationModelAssembler<ArticleResponse, EntityModel<ArticleResponse>> {

    /** 게시판 목록 링크에 사용할 기본 페이지 크기 (application.yml default-page-size 와 동일하게 유지) */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    @Override
    public EntityModel<ArticleResponse> toModel(ArticleResponse article) {
        return EntityModel.of(article,
                /*
                 * linkTo(methodOn(Controller.class).method(args))
                 * => methodOn 이 컨트롤러 프록시를 만들고, 메서드 호출 정보(@RequestMapping + @PathVariable 값)를 기록한다.
                 * => linkTo 가 기록된 정보로 현재 요청의 scheme/host/port 를 포함한 절대 URL 을 만든다.
                 * => URL 문자열을 하드코딩하지 않으므로 컨트롤러 경로가 바뀌어도 링크가 자동으로 따라간다.
                 */
                linkTo(methodOn(ArticleHateoasController.class).readArticle(article.getArticleId())).withSelfRel(),
                boardArticlesLink(article.getBoardId()),
                linkTo(methodOn(ArticleHateoasController.class).boardArticleCount(article.getBoardId())).withRel("board-article-count")
        );
    }

    /**
     * 게시판 목록 첫 페이지 링크 (rel = "articles")
     *
     * - 목록 API 는 Pageable 파라미터를 받는다.
     *   methodOn 으로 Pageable 인자를 넘기면 쿼리 파라미터 변환이 UriComponentsContributor 등록 여부에 따라 달라질 수 있으므로,
     *   컨트롤러의 기본 경로(linkTo(Class))만 가져온 뒤 쿼리 파라미터를 명시적으로 붙인다.
     * - page=1 은 one-indexed-parameters: true 기준의 첫 페이지이다.
     */
    public static Link boardArticlesLink(Long boardId) {
        String href = linkTo(ArticleHateoasController.class).toUriComponentsBuilder()
                .queryParam("boardId", boardId)
                .queryParam("page", 1)
                .queryParam("size", DEFAULT_PAGE_SIZE)
                .build()
                .toUriString();
        return Link.of(href, "articles");
    }
}
