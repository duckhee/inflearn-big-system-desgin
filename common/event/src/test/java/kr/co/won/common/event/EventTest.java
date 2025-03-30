package kr.co.won.common.event;

import kr.co.won.common.event.payload.ArticleCreateEventPayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @DisplayName(value = "01. 역직렬화 테스트")
    @Test
    void deserializedTests() {
        ArticleCreateEventPayload creatPayload = ArticleCreateEventPayload.builder()
                .articleId(1l)
                .title("title")
                .content("content")
                .boardId(1l)
                .writerId(1l)
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .boardArticleCount(23l)
                .build();

        Event<EventPayload> createEvent = Event.of(1234l, EventType.ARTICLE_CREATE, creatPayload);

        String json = createEvent.toJson();
        System.out.println("json = " + json);

        Event<EventPayload> result = Event.fromJson(json);

        Assertions.assertThat(result.getEventId()).isEqualTo(createEvent.getEventId());
        Assertions.assertThat(result.getType()).isEqualTo(createEvent.getType());
        Assertions.assertThat(result.getPayload()).isInstanceOf(creatPayload.getClass());

        ArticleCreateEventPayload resultPayload = (ArticleCreateEventPayload) result.getPayload();
        Assertions.assertThat(resultPayload.getArticleId()).isEqualTo(creatPayload.getArticleId());
        Assertions.assertThat(resultPayload.getTitle()).isEqualTo(creatPayload.getTitle());
        Assertions.assertThat(resultPayload.getCreatedAt()).isEqualTo(creatPayload.getCreatedAt());

    }

}