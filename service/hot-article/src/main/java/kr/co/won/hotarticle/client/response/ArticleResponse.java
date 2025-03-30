package kr.co.won.hotarticle.client.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArticleResponse {

    private Long articleId;

    private String title;

    private LocalDateTime createdAt;

}
