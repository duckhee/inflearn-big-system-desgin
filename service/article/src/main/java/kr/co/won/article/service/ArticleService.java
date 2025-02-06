package kr.co.won.article.service;

import kr.co.won.article.entity.ArticleEntity;
import kr.co.won.article.repository.ArticleRepository;
import kr.co.won.article.service.request.ArticleCreateRequest;
import kr.co.won.article.service.request.ArticleUpdateRequest;
import kr.co.won.article.service.response.ArticleResponse;
import kr.co.won.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final Snowflake snowflake = new Snowflake();
    private final ArticleRepository articleRepository;

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

    @Transactional
    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
    }
}
