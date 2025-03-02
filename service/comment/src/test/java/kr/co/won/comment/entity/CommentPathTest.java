package kr.co.won.comment.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class CommentPathTest {

    @DisplayName(value = "01. 자식 path에 대한 구하는 기능 테스트 -> 최초 생성 ")
    @Test
    void createChildPathTests() {
        createChildCommentTests(CommentPath.create(""), null, "00000");
    }

    @DisplayName(value = "02. 최초로 댓글이 자식 댓글이 생성이 되는 경우 테스트")
    @Test
    void createChildFirstCommentTests() {
        createChildCommentTests(CommentPath.create("00000"), null, "0000000000");
    }

    @DisplayName(value = "03. 한 개의 댓글이 있을 때 생성이 되는 경우 테스트")
    @Test
    void createSecondCommentTests() {
        createChildCommentTests(CommentPath.create(""), "00000", "00001");
    }

    @DisplayName(value = "04. 한 개의 댓글이 있고 한 개의 댓글에 여러개의 댓글이 달려 있는 상황에서 댓글이 추가가 되는 경우 테스트")
    @Test
    void createMultiCommentsTests() {
        createChildCommentTests(CommentPath.create("0000z"), "0000zabcdzzzzzz", "0000zabce0");
    }

    void createChildCommentTests(CommentPath commentPath, String descendentsTopPath, String expectedChilePath) {
        CommentPath childCommentPath = commentPath.getChildCommentPath(descendentsTopPath);
        assertThat(childCommentPath.getPath()).isEqualTo(expectedChilePath);
    }

    @DisplayName(value = "05. 최대 댓글에 대한 Depth에 대한 에러 발생 확인 테스트")
    @Test
    void createChildCommentPathIfMaxDepthTests() {
        assertThatThrownBy(() -> CommentPath.create("zzzzz".repeat(5)).getChildCommentPath(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName(value = "06. 최대 표현 가능한 CHUK 단위를 벗어났을 경우에 대한 테스트")
    @Test
    void createChildCommentChunkIfMaxDepthTests() {
        // given
        CommentPath commentPath = CommentPath.create("");

        // when, then
        assertThatThrownBy(() -> commentPath.getChildCommentPath("zzzzz")).isInstanceOf(IllegalArgumentException.class);
    }

}