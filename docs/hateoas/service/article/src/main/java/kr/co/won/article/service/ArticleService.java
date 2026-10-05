package kr.co.won.article.service;

import kr.co.won.article.entity.ArticleEntity;
import kr.co.won.article.entity.BoardArticleCountEntity;
import kr.co.won.article.repository.ArticleRepository;
import kr.co.won.article.repository.BoardArticleCountRepository;
import kr.co.won.article.service.request.ArticleCreateRequest;
import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticlePageResponse;
import kr.co.won.article.service.response.ArticleResponse;
import kr.co.won.article.service.utils.paging.PageLimitCalculator;
import kr.co.won.common.event.EventType;
import kr.co.won.common.event.payload.ArticleCreateEventPayload;
import kr.co.won.common.event.payload.ArticleDeletedEventPayload;
import kr.co.won.common.event.payload.ArticleUpdateEventPayload;
import kr.co.won.common.outboxmessagerelay.event.OutboxEventPublisher;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 비즈니스 로직 [원본 코드 유지]
 *
 * [HATEOAS 마이그레이션 원칙]
 * - Service 는 HATEOAS(링크, PagedModel)를 전혀 알지 못한다.
 * - v1 / v2 컨트롤러가 같은 Service 메서드를 호출하고, 링크는 v2 컨트롤러(웹 계층)에서만 붙인다.
 *   => 비즈니스 로직 / 쿼리 / 이벤트 발행(Outbox) 동작이 v1 과 100% 동일하게 유지된다.
 */
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;
    private final BoardArticleCountRepository articleCountRepository;
    // Event 를 발행하기 위한 서비스 (Transactional Outbox -> Kafka)
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * 게시글 생성 + 게시판 게시글 수 증가 + ARTICLE_CREATE 이벤트 발행
     */
    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest request) {
        ArticleEntity article = articleRepository.save(
                ArticleEntity.createArticle(
                        snowflake.nextId(),
                        request.getTitle(),
                        request.getContent(),
                        request.getBoardId(),
                        request.getWriterId()
                )
        );

        /** 게시글에 대한 수 증가 (row 가 없으면 최초 1 로 생성) */
        int articleCount = articleCountRepository.increaseArticleCount(request.getBoardId());
        if (articleCount == 0) {
            articleCountRepository.save(BoardArticleCountEntity.init(request.getBoardId(), 1l));
        }
        // kafka로 이벤트 발행
        outboxEventPublisher.publish(
                EventType.ARTICLE_CREATE,
                ArticleCreateEventPayload.builder()
                        .articleId(article.getArticleId())
                        .title(article.getTitle())
                        .content(article.getContent())
                        .boardId(article.getBoardId())
                        .writerId(article.getWriterId())
                        .createdAt(article.getCreatedAt())
                        .modifiedAt(article.getModifiedAt())
                        .boardArticleCount(articleCountNumber(article.getBoardId()))
                        .build(),
                article.getBoardId()
        );
        return ArticleResponse.fromEntity(article);
    }

    /**
     * JPA dirty checking 으로 정보 수정 + ARTICLE_UPDATE 이벤트 발행
     */
    @Transactional
    public ArticleResponse updateArticle(Long articleId, ArticleUpdateRequest request) {
        ArticleEntity findArticle = articleRepository.findById(articleId)
                .orElseThrow();
        findArticle.updateArticle(request.getTitle(), request.getContent());

        // kafka로 이벤트 발행
        outboxEventPublisher.publish(
                EventType.ARTICLE_UPDATE,
                ArticleUpdateEventPayload.builder()
                        .articleId(findArticle.getArticleId())
                        .title(findArticle.getTitle())
                        .content(findArticle.getContent())
                        .boardId(findArticle.getBoardId())
                        .writerId(findArticle.getWriterId())
                        .createdAt(findArticle.getCreatedAt())
                        .modifiedAt(findArticle.getModifiedAt())
                        .build(),
                findArticle.getBoardId()
        );

        return ArticleResponse.fromEntity(findArticle);
    }

    /**
     * 게시글 단건 조회
     */
    public ArticleResponse findArticle(Long articleId) {
        ArticleEntity findArticle = articleRepository.findById(articleId).orElseThrow();
        return ArticleResponse.fromEntity(findArticle);
    }

    /**
     * 일반적으로 사용을 하는 페이지 번호가 보이는 페이징 처리
     *
     * @param boardId    게시판 ID
     * @param pageNumber 1-based 페이지 번호 (v2 컨트롤러는 Pageable 의 0-based 번호에 +1 해서 넘긴다)
     * @param pageSize   페이지 크기
     * @return 현재 페이지 목록 + 블록 제한 count
     */
    public ArticlePageResponse pageArticle(Long boardId, Long pageNumber, Long pageSize) {
        Long pageLimitCount = PageLimitCalculator.calculatePageLimit(pageNumber, pageSize, 10L);
        Long countPage = articleRepository.countPage(boardId, pageLimitCount);
        long pageOffset = (pageNumber - 1) * pageSize;
        List<ArticleResponse> pageArticleResponse = articleRepository.pagingQuery(boardId, pageOffset, pageSize).stream()
                .map(ArticleResponse::fromEntity).toList();
        return ArticlePageResponse.of(pageArticleResponse, countPage);
    }

    /**
     * 무한 스크롤 형태로 사용을 할 때 사용을 하는 리스트 처리 (cursor = lastArticleId)
     *
     * @param boardId       게시판 ID
     * @param pageSize      조회 개수
     * @param lastArticleId 이전 응답의 마지막 게시글 ID (null 이면 처음부터)
     */
    public List<ArticleResponse> infinityScrollArticle(Long boardId, Long pageSize, Long lastArticleId) {
        List<ArticleEntity> articleEntities = lastArticleId == null ? articleRepository.findAllInfinityScroll(boardId, pageSize) : articleRepository.findAllInfinityScroll(boardId, pageSize, lastArticleId);
        return articleEntities.stream().map(ArticleResponse::fromEntity).toList();
    }

    /**
     * 게시글 삭제 + 게시판 게시글 수 감소 + ARTICLE_DELETE 이벤트 발행
     */
    @Transactional
    public void deleteArticle(Long articleId) {
        ArticleEntity findArticle = articleRepository.findById(articleId).orElseThrow();
        articleRepository.deleteById(articleId);
        /** 게시글 수 감소 */
        articleCountRepository.decreaseArticleCount(findArticle.getBoardId());

        // kafka로 이벤트 발행
        outboxEventPublisher.publish(
                EventType.ARTICLE_DELETE,
                ArticleDeletedEventPayload.builder()
                        .articleId(findArticle.getArticleId())
                        .title(findArticle.getTitle())
                        .content(findArticle.getContent())
                        .boardId(findArticle.getBoardId())
                        .writerId(findArticle.getWriterId())
                        .createdAt(findArticle.getCreatedAt())
                        .modifiedAt(findArticle.getModifiedAt())
                        .boardArticleCount(articleCountNumber(findArticle.getBoardId()))
                        .build(),
                findArticle.getBoardId()
        );
    }

    /**
     * 게시판의 전체 게시글 수 (row 가 없으면 0)
     */
    public Long articleCountNumber(Long boardId) {
        return articleCountRepository.findById(boardId)
                .map(BoardArticleCountEntity::getArticleCount)
                .orElse(0l);
    }

}
