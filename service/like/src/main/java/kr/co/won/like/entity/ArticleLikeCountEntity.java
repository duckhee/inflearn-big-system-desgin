package kr.co.won.like.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Table(name = "tbl_article_like_count")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLikeCountEntity {

    @Id
    private Long articleId; // primary key and shard key

    private Long likeCount;

    /**
     * OPTIMISTIC Lock(낙관적 락)에서 사용할 버전 값
     */
    @Version
    private Long version = 0l;

    /**
     * 초기화를 하기 위해서 사용이 되는 Factory Method
     */
    public static ArticleLikeCountEntity init(Long articleId, Long likeCount) {
        ArticleLikeCountEntity articleLikeCountEntity = new ArticleLikeCountEntity();
        articleLikeCountEntity.articleId = articleId;
        articleLikeCountEntity.likeCount = likeCount;
//        articleLikeCountEntity.version = 0l;
        return articleLikeCountEntity;
    }

    /**
     * 좋아요 수를 증가 시켜 주는 함수
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수를 감소 시켜 주는 함수
     */
    public void decreaseLikeCount() {
        this.likeCount--;
    }
}
