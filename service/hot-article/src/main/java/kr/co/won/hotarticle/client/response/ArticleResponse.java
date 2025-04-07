package kr.co.won.hotarticle.client.response;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class ArticleResponse {

    private Long articleId;

    private String title;

    private LocalDateTime createdAt;

}
