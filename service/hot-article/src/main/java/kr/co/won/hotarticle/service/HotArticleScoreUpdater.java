package kr.co.won.hotarticle.service;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventPayload;
import kr.co.won.hotarticle.repository.ArticleCreatedTimeRepository;
import kr.co.won.hotarticle.repository.HotArticleListRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HotArticleScoreUpdater {

    private final HotArticleListRepository hotArticleListRepository;

    private final HotArticleScoreCalculator hotArticleScoreCalculator;

    private final ArticleCreatedTimeRepository articleCreatedTimeRepository;

    private final static long HOT_ARTICLE_COUNT = 10;

    // 10일동안 저장을 하기 우히나 상수 지정
    private final static Duration HOT_ARTICLE_TTL = Duration.ofDays(10);

    public void update(Event<EventPayload> event, EventHandler<EventPayload> eventHandler) {
        Long articleId = eventHandler.findArticleId(event);
        LocalDateTime createdArticleTime = articleCreatedTimeRepository.read(articleId);
        if (!isArticleCreatedToDay(createdArticleTime)) {
            return;
        }

        eventHandler.handle(event);

        long score = hotArticleScoreCalculator.calculate(articleId);
        hotArticleListRepository.add(
                articleId,
                createdArticleTime,
                score,
                HOT_ARTICLE_COUNT,
                HOT_ARTICLE_TTL
        );
    }

    /**
     * 오늘 날짜로 생성이 된 게시글인지 확인을 하는 함수
     *
     * @param createdArticleTime
     * @return
     */
    private boolean isArticleCreatedToDay(LocalDateTime createdArticleTime) {
        return createdArticleTime != null && createdArticleTime.toLocalDate().equals(LocalDate.now());
    }
}
