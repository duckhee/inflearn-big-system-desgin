package kr.co.won.comment.service;

import kr.co.won.comment.entity.CommentEntity;
import kr.co.won.comment.repository.CommentRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;

    @DisplayName(value = "01. 삭제할 댓글이 대댓글이 있으면, 삭제 표시만 한다.")
    @Test
    void deleteShouldMarkedDeletedIfHasSubCommentTests() {
        // given
        Long articleId = 1l;
        Long commentId = 2l;
        CommentEntity mockComment = createComment(articleId, commentId);
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(mockComment));
        given(commentRepository.checkSubComment(articleId, commentId, 2l))
                .willReturn(2L);

        // when
        commentService.deleteComment(commentId);

        // then
        verify(mockComment).deleteComment();
    }

    @DisplayName(value = "02. 하위 댓글이 삭제가 되고, 삭제 되지 않은 부모라면, 하위 댓글만 삭제가 된다.")
    @Test
    void deleteShouldDeleteSubCommentIfNotDeletedParentTests() {
        // given
        Long articleId = 1l;
        Long commentId = 2l;
        Long parentCommentId = 1l;

        CommentEntity mockComment = createComment(articleId, commentId, parentCommentId);

        given(mockComment.isRoot()).willReturn(false);

        CommentEntity parentComment = mock(CommentEntity.class);
        given(parentComment.getDeleted()).willReturn(false);


        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(mockComment));
        given(commentRepository.checkSubComment(articleId, commentId, 2l))
                .willReturn(1L);

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));

        // when
        commentService.deleteComment(commentId);

        // then
        verify(commentRepository).delete(mockComment);
        verify(commentRepository, never()).delete(parentComment);
    }

    @DisplayName(value = "03. 하위 댓글이 삭제가 되고, 삭제된 부모라면, 재귀적으로 모두 삭제한다.")
    @Test
    void deleteShouldDeleteAllRecursivelyIfDeletedParentTests() {
        // given
        Long articleId = 1l;
        Long commentId = 2l;
        Long parentCommentId = 1l;

        CommentEntity mockComment = createComment(articleId, commentId, parentCommentId);

        given(mockComment.isRoot()).willReturn(false);

        CommentEntity parentComment = createComment(articleId, parentCommentId);
        given(parentComment.isRoot()).willReturn(true);
        given(parentComment.getDeleted()).willReturn(true);


        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(mockComment));
        given(commentRepository.checkSubComment(articleId, commentId, 2l))
                .willReturn(1L);

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));
        given(commentRepository.checkSubComment(articleId, parentCommentId, 2l))
                .willReturn(1L);

        // when
        commentService.deleteComment(commentId);

        // then
        verify(commentRepository).delete(mockComment);
        verify(commentRepository).delete(parentComment);
    }

    private CommentEntity createComment(Long articleId, Long commentId) {
        CommentEntity mockCommentEntity = mock(CommentEntity.class);
        given(mockCommentEntity.getArticleId()).willReturn(articleId);
        given(mockCommentEntity.getCommentId()).willReturn(commentId);
        return mockCommentEntity;
    }

    private CommentEntity createComment(Long articleId, Long commentId, Long parentCommentId) {
        CommentEntity mockCommentEntity = createComment(articleId, commentId);
        given(mockCommentEntity.getParentCommentId()).willReturn(parentCommentId);
        return mockCommentEntity;
    }
}