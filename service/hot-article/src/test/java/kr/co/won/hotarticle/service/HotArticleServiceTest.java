package kr.co.won.hotarticle.service;

import kr.co.won.common.event.Event;
import kr.co.won.common.event.EventType;
import kr.co.won.hotarticle.client.ArticleClient;
import kr.co.won.hotarticle.repository.HotArticleListRepository;
import kr.co.won.hotarticle.service.handler.EventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotArticleServiceTest {

    @InjectMocks
    HotArticleService hotArticleService;

    @Mock
    List<EventHandler> eventHandlers;

    @Mock
    HotArticleScoreUpdater hotArticleScoreUpdater;

    @DisplayName(value = "01. 이벤트 핸들러가 없는 경우 테스트")
    @Test
    void notHaveEventHandlerTests() {
        // given
        Event mockEvent = mock(Event.class);
        EventHandler handler = mock(EventHandler.class);

        given(handler.isSupport(mockEvent)).willReturn(false);
        given(eventHandlers.stream()).willReturn(Stream.of(handler));

        // when
        hotArticleService.handleEvent(mockEvent);

        verify(handler, never()).handle(mockEvent);
        verify(hotArticleScoreUpdater, never()).update(mockEvent, handler);
    }

    @DisplayName(value = "02. 이벤트 핸들러가 있는 경우 테스트 -> 게시글 생성")
    @Test
    void haveCreateArticleEventHandlerTests() {
        // given
        Event mockEvent = mock(Event.class);
        given(mockEvent.getType()).willReturn(EventType.ARTICLE_CREATE);
        EventHandler handler = mock(EventHandler.class);


        given(handler.isSupport(mockEvent)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(handler));

        // when
        hotArticleService.handleEvent(mockEvent);

        verify(handler, times(1)).handle(mockEvent);
        verify(hotArticleScoreUpdater, never()).update(mockEvent, handler);
    }

    @DisplayName(value = "03. 이벤트 핸들러가 있는 경우 테스트 -> 게시글 삭제")
    @Test
    void haveDeletedArticleEventHandlerTests() {
        // given
        Event event = mock(Event.class);
        given(event.getType()).willReturn(EventType.ARTICLE_DELETE);

        EventHandler eventHandler = mock(EventHandler.class);
        given(eventHandler.isSupport(event)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(eventHandler));

        // when
        hotArticleService.handleEvent(event);

        // then
        verify(eventHandler).handle(event);
        verify(hotArticleScoreUpdater, never()).update(event, eventHandler);
    }

    @DisplayName(value = "04. 스코어가 변경이 가능한 경우에 대한 테스트")
    @Test
    void handleScoreUpdateEventHandlerTests() {
        // given
        Event mockEvent = mock(Event.class);
        given(mockEvent.getType()).willReturn(mock(EventType.class));

        EventHandler handler = mock(EventHandler.class);
        given(handler.isSupport(mockEvent)).willReturn(true);
        given(eventHandlers.stream()).willReturn(Stream.of(handler));

        // when
        hotArticleService.handleEvent(mockEvent);

        verify(handler, never()).handle(mockEvent);
        verify(hotArticleScoreUpdater).update(mockEvent, handler);
    }
}