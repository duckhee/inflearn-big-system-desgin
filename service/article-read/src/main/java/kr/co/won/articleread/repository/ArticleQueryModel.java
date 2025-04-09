package kr.co.won.articleread.repository;

import kr.co.won.articleread.client.ArticleClient;
import kr.co.won.common.event.payload.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArticleQueryModel {

    private Long articleId;

    private String title;
    private String content;

    private Long boardId;

    private Long writerId;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private Long articleCommentCount;

    private Long articleLikeCount;

    public static ArticleQueryModel create(ArticleCreateEventPayload payload) {
        ArticleQueryModel articleQueryModel = new ArticleQueryModel();
        articleQueryModel.articleId = payload.getArticleId();
        articleQueryModel.title = payload.getTitle();
        articleQueryModel.content = payload.getContent();
        articleQueryModel.boardId = payload.getBoardId();
        articleQueryModel.writerId = payload.getWriterId();
        articleQueryModel.createdAt = payload.getCreatedAt();
        articleQueryModel.modifiedAt = payload.getModifiedAt();
        articleQueryModel.articleCommentCount = 0l;
        articleQueryModel.articleLikeCount = 0l;
        return articleQueryModel;
    }

    public static ArticleQueryModel create(ArticleClient.ArticleResponse response, Long commentCount, Long likeCount) {
        ArticleQueryModel articleQueryModel = new ArticleQueryModel();
        articleQueryModel.articleId = response.getArticleId();
        articleQueryModel.title = response.getTitle();
        articleQueryModel.content = response.getContent();
        articleQueryModel.boardId = response.getBoardId();
        articleQueryModel.writerId = response.getWriterId();
        articleQueryModel.createdAt = response.getCreatedAt();
        articleQueryModel.modifiedAt = response.getModifiedAt();
        articleQueryModel.articleLikeCount = likeCount;
        articleQueryModel.articleCommentCount = commentCount;
        return articleQueryModel;
    }

    public void updateCommentCountByEvent(CommentCreatedEventPayload payload) {
        this.articleCommentCount = payload.getArticleCommentCount();
    }

    public void updateCommentCountByEvent(CommentDeletedEventPayload payload) {
        this.articleCommentCount = payload.getArticleCommentCount();
    }

    public void updateLikeCountByEvent(ArticleLikedEventPayload payload) {
        this.articleLikeCount = payload.getArticleLikeCount();
    }

    public void updateLikeCountByEvent(ArticleUnLikedEventPayload payload) {
        this.articleLikeCount = payload.getArticleLikeCount();
    }

    public void updateArticleByEvent(ArticleUpdateEventPayload payload) {
        this.title = payload.getTitle();
        this.content = payload.getContent();
        this.boardId = payload.getBoardId();
        this.writerId = payload.getWriterId();
        this.createdAt = payload.getCreatedAt();
        this.modifiedAt = payload.getModifiedAt();
    }
}
