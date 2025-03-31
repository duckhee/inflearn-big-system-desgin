package kr.co.won.hotarticle.controller;

import kr.co.won.hotarticle.service.HotArticleService;
import kr.co.won.hotarticle.service.response.HotArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/hot-articles")
@RequiredArgsConstructor
public class HotArticleController {

    private final HotArticleService hotArticleService;

    /**
     * 특정 날짜에 있는 인기글 가져 오는 GET Method
     *
     * @param dateStr
     * @return
     */
    @GetMapping(path = "/articles/date/{dateStr}")
    public List<HotArticleResponse> findHotArticleListResponse(@PathVariable(name = "dateStr") String dateStr) {
        return hotArticleService.readAll(dateStr);
    }
}
