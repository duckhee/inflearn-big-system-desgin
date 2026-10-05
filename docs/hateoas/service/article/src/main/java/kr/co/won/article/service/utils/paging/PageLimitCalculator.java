package kr.co.won.article.service.utils.paging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 페이지 블록 단위 count limit 계산기 [원본 코드 유지]
 *
 * 예) pageSize = 10, movablePageCount = 10
 *   page 1 ~ 10  -> 101  (1~10 페이지 100건 + "다음 블록이 있는지" 확인용 1건)
 *   page 11 ~ 20 -> 201
 *
 * [HATEOAS 와의 관계]
 * - 이 값으로 센 count 가 PagedModel 의 totalElements 가 되고, totalPages = ceil(totalElements / size) 가 된다.
 * - 따라서 데이터가 많은 게시판에서 last 링크는 "진짜 마지막 페이지"가 아니라 "다음 블록의 첫 페이지"를 가리킨다.
 *   (page 1~10 요청 시 last = page 11) => "페이지 번호 10개 + 다음 버튼" UI 를 위한 의도된 동작
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimitCalculator {

    /**
     * @param pageNumber       현재 페이지 번호 (1-based)
     * @param pageSize         한 페이지 당 보여줄 게시글의 수
     * @param movablePageCount 이동 가능한 페이지 번호의 갯수 -> 화면 상에 보여줄 페이지 번호
     * @return count 쿼리에 사용할 limit
     */
    public static Long calculatePageLimit(Long pageNumber, Long pageSize, Long movablePageCount) {
        return (((pageNumber - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
    }
}
