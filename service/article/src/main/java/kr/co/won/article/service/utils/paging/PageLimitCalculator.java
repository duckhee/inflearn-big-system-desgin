package kr.co.won.article.service.utils.paging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLimitCalculator {

    /**
     * @param pageNumber       현재 페이지 번호
     * @param pageSize         한 페이지 당 보여줄 게시글의 수
     * @param movablePageCount 이동 가능한 페이지 번호의 갯수 -> 화면 상에 보여줄 페이지 번호
     * @return
     */
    public static Long calculatePageLimit(Long pageNumber, Long pageSize, Long movablePageCount) {
        return (((pageNumber - 1) / movablePageCount) + 1) * pageSize * movablePageCount + 1;
    }
}
