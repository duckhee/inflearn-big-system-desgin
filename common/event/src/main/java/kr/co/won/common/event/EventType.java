package kr.co.won.common.event;

import kr.co.won.common.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    ARTICLE_CREATE(ArticleCreateEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    ARTICLE_UPDATE(ArticleUpdateEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    ARTICLE_DELETE(ArticleDeletedEventPayload.class, Topic.KUKE_BOARD_ARTICLE),
    COMMENT_CREATE(CommentCreatedEventPayload.class, Topic.KUKE_BOARD_COMMENT),
    COMMENT_DELETE(CommentCreatedEventPayload.class, Topic.KUKE_BOARD_COMMENT),
    ARTICLE_LIKE(ArticleLikedEventPayload.class, Topic.KUKE_BOARD_LIKE),
    ARTICLE_UNLIKE(ArticleUnLikedEventPayload.class, Topic.KUKE_BOARD_LIKE),
    ARTICLE_VIEWED(ArticleViewedEventPayload.class, Topic.KUKE_BOARD_VIEW);

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            log.error("[EventType.from] type={}", type, e);
            return null;
        }
    }

    public static class Topic {
        public static final String KUKE_BOARD_ARTICLE = "kuke-board-article";
        public static final String KUKE_BOARD_COMMENT = "kuke-board-comment";
        public static final String KUKE_BOARD_LIKE = "kuke-board-like";
        public static final String KUKE_BOARD_VIEW = "kuke-board-view";

    }


}
