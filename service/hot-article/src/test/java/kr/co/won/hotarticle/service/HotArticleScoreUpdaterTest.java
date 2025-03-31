package kr.co.won.hotarticle.service;

import kr.co.won.common.event.Event;
import kr.co.won.hotarticle.repository.ArticleCreatedTimeRepository;
import kr.co.won.hotarticle.repository.HotArticleListRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotArticleScoreUpdaterTest {

    @InjectMocks
    HotArticleScoreUpdater hotArticleScoreUpdater;

    @Mock
    HotArticleListRepository hotArticleListRepository;

    @Mock
    HotArticleScoreCalculator hotArticleScoreCalculator;

    @Mock
    ArticleCreatedTimeRepository articleCreatedTimeRepository;

    @DisplayName(value = "01. 게시글이 오늘 생성이 되지 않았을 때 경우 테스트")
    @Test
    void updateNotToDayCreateArticleTests() {
        Long articleId = 1l;
        // 가짜 객체 생성
        Event event = mock(Event.class);
        EventHandler handler = mock(EventHandler.class);
        given(handler.findArticleId(event)).willReturn(articleId);

        LocalDateTime createArticleTime = LocalDateTime.now().minusDays(1l);
        given(articleCreatedTimeRepository.read(articleId)).willReturn(createArticleTime);

        // when
        hotArticleScoreUpdater.update(event, handler);

        // then
        verify(handler, never()).handle(event);
        verify(hotArticleListRepository, never()).add(anyLong(), any(LocalDateTime.class), anyLong(), anyLong(), any(Duration.class));

    }

    @DisplayName(value = "02. 게시글이 오늘 생성이 된 경우 테스트")
    @Test
    void updateTodayCreateArticleTests() {
        Long articleId = 1l;
        // 가짜 객체 생성
        Event event = mock(Event.class);
        EventHandler handler = mock(EventHandler.class);
        given(handler.findArticleId(event)).willReturn(articleId);

        LocalDateTime createArticleTime = LocalDateTime.now().minusMinutes(30l);
        given(articleCreatedTimeRepository.read(articleId)).willReturn(createArticleTime);

        // when
        hotArticleScoreUpdater.update(event, handler);

        // then
        verify(handler, times(1)).handle(event);
        verify(hotArticleListRepository, times(1)).add(anyLong(), any(LocalDateTime.class), anyLong(), anyLong(), any(Duration.class));
    }

}