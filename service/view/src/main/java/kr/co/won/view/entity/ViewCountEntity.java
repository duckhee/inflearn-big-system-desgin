package kr.co.won.view.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_article_view_count")
public class ViewCountEntity {

    @Id
    private Long articleId; // shard key

    private Long viewCount;


    public static ViewCountEntity Init(Long articleId, Long viewCount) {
        ViewCountEntity viewCountEntity = new ViewCountEntity();
        viewCountEntity.articleId = articleId;
        viewCountEntity.viewCount = viewCount;
        return viewCountEntity;
    }

}
