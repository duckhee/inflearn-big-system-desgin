package kr.co.won.hotarticle.service;

import kr.co.won.hotarticle.repository.ArticleCommentCountRepository;
import kr.co.won.hotarticle.repository.ArticleLikeCountRepository;
import kr.co.won.hotarticle.repository.ArticleViewCountRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HotArticleScoreCalculatorTest {

    @InjectMocks
    HotArticleScoreCalculator hotArticleScoreCalculator;

    @Mock
    private ArticleViewCountRepository articleViewCountRepository;

    @Mock
    private ArticleCommentCountRepository articleCommentCountRepository;

    @Mock
    private ArticleLikeCountRepository articleLikeCountRepository;


    @DisplayName(value = "01. 스코어에 대해서 계산을 하는 기능 테스트")
    @Test
    void calculateTests() {
        // given
        long articleId = 1l;
        // 100 이하의 난수 생성
        long likeCount = RandomGenerator.getDefault().nextLong(100);
        long commentCount = RandomGenerator.getDefault().nextLong(100);
        long viewCount = RandomGenerator.getDefault().nextLong(100);

        given(articleLikeCountRepository.read(articleId)).willReturn(likeCount);
        given(articleCommentCountRepository.read(articleId)).willReturn(commentCount);
        given(articleViewCountRepository.read(articleId)).willReturn(viewCount);

        long score = hotArticleScoreCalculator.calculate(articleId);

        Assertions.assertThat(score)
                .isEqualTo(3 * likeCount + 2 * commentCount + 1 * viewCount);

        verify(articleLikeCountRepository, times(1)).read(articleId);
        verify(articleCommentCountRepository, times(1)).read(articleId);
        verify(articleViewCountRepository, times(1)).read(articleId);


    }
}