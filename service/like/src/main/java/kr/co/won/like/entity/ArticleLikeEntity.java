package kr.co.won.like.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@Table(name = "tbl_article_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLikeEntity {

    @Id
    private Long articleLikeId;

    private Long articleId; // shard key

    private Long userId;

    private LocalDateTime createdAt;

    public static ArticleLikeEntity create(Long articleLikeId, Long articleId, Long userId) {
        ArticleLikeEntity articleLikeEntity = new ArticleLikeEntity();
        articleLikeEntity.articleLikeId = articleLikeId;
        articleLikeEntity.articleId = articleId;
        articleLikeEntity.userId = userId;
        articleLikeEntity.createdAt = LocalDateTime.now();
        return articleLikeEntity;
    }

}
