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
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;
    private final BoardArticleCountRepository articleCountRepository;

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

        /** 게시글에 대한 수 증가 */
        int articleCount = articleCountRepository.increaseArticleCount(request.getBoardId());
        if (articleCount == 0) {
            articleCountRepository.save(BoardArticleCountEntity.init(request.getBoardId(), 1l));
        }

        return ArticleResponse.fromEntity(article);
    }

    /**
     * JPA dirty checking 으로 정보 수정
     */
    @Transactional
    public ArticleResponse updateArticle(Long articleId, ArticleUpdateRequest request) {
        ArticleEntity findArticle = articleRepository.findById(articleId)
                .orElseThrow();
        findArticle.updateArticle(request.getTitle(), request.getContent());

        return ArticleResponse.fromEntity(findArticle);
    }


    public ArticleResponse findArticle(Long articleId) {
        ArticleEntity findArticle = articleRepository.findById(articleId).orElseThrow();
        return ArticleResponse.fromEntity(findArticle);
    }

    /**
     * 일반적으로 사용을 하는 페이지 번호가 보이는 페이징 처리
     *
     * @param boardId
     * @param pageNumber
     * @param pageSize
     * @return
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
     * 무한 스크롤 형태로 사용을 할 때 사용을 하는 리스트 처리
     *
     * @param boardId
     * @param pageSize
     * @param lastArticleId
     * @return
     */
    public List<ArticleResponse> infinityScrollArticle(Long boardId, Long pageSize, Long lastArticleId) {
        List<ArticleEntity> articleEntities = lastArticleId == null ? articleRepository.findAllInfinityScroll(boardId, pageSize) : articleRepository.findAllInfinityScroll(boardId, pageSize, lastArticleId);
        return articleEntities.stream().map(ArticleResponse::fromEntity).toList();
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        ArticleEntity findArticle = articleRepository.findById(articleId).orElseThrow();
        articleRepository.deleteById(articleId);
        /** 게시글 수 감소 */
        articleCountRepository.decreaseArticleCount(findArticle.getBoardId());
    }

    /**
     * 게시글의 수를 반환을 하는 함수
     *
     * @param boardId
     * @return
     */
    public Long articleCountNumber(Long boardId) {
        return articleCountRepository.findById(boardId)
                .map(BoardArticleCountEntity::getArticleCount)
                .orElse(0l);
    }
}
