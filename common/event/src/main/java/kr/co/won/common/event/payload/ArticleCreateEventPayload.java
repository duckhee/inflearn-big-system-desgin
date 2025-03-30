package kr.co.won.common.event.payload;

import kr.co.won.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateEventPayload implements EventPayload {

    private Long articleId;

    private String title;

    private String content;

    private Long boardId;

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private Long boardArticleCount;

}
