package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.comment.CommentRequest;
import com.walking.backend.domain.dto.comment.CommentResponse;
import com.walking.backend.domain.model.Comment;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.repository.CommentRepository;
import com.walking.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser
@RequiredArgsConstructor
public class CommentServiceIT extends IntegrationTestBase {
    private final CommentService commentService;
    private final CommentRepository commentRepository;

    @Test
    void getComments_whenTaskExistsAndUserHasAccess_shouldReturnCommentsPage() {
        var pageable = PageRequest.of(0, 10);

        Page<CommentResponse> actual = commentService.getComments(1L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(CommentResponse::content)
                .contains("Test comment content");
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getComments_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> commentService.getComments(1L, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getComments_whenTaskHasNoComments_shouldReturnEmptyPage() {
        var pageable = PageRequest.of(0, 10);

        Page<CommentResponse> actual = commentService.getComments(3L, pageable);

        assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
    }

    @Test
    void createComment_whenValidRequestAndUserCanViewTask_shouldCreateComment() {
        var request = new CommentRequest("New test comment");

        CommentResponse response = commentService.createComment(1L, 2L, request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("New test comment");
        assertThat(response.author().username()).isEqualTo("jane_smith");

        Optional<Comment> saved = commentRepository.findById(response.id());
        assertThat(saved).isPresent();
        assertThat(saved.get().getContent()).isEqualTo("New test comment");
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void createComment_whenUserCannotViewTask_shouldThrowAccessDeniedException() {
        var request = new CommentRequest("New test comment");

        assertThatThrownBy(() -> commentService.createComment(1L, 99L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe") // author of comment 1
    void updateComment_whenUserIsAuthor_shouldUpdateComment() {
        var request = new CommentRequest("Updated comment content");

        CommentResponse response = commentService.updateComment(1L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Updated comment content");

        Comment updated = commentRepository.findById(1L).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("Updated comment content");
    }

    @Test
    void updateComment_whenUserIsBoardOwnerButNotAuthor_shouldThrowAccessDenied() {
        var request = new CommentRequest("Owner trying to edit");

        assertThatThrownBy(() -> commentService.updateComment(1L, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void updateComment_whenUserIsEditorButNotAuthor_shouldThrowAccessDenied() {
        var request = new CommentRequest("Editor trying to edit");

        assertThatThrownBy(() -> commentService.updateComment(1L, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void updateComment_whenUserCannotEditComment_shouldThrowAccessDenied() {
        var request = new CommentRequest("Hacked content");

        assertThatThrownBy(() -> commentService.updateComment(1L, 1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void updateComment_whenCommentNotFound_shouldThrowAccessDenied() {
        var request = new CommentRequest("New content");

        assertThatThrownBy(() -> commentService.updateComment(1L, 999L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe") // author of comment 1
    void deleteComment_whenUserIsAuthor_shouldDeleteComment() {
        commentService.deleteComment(1L, 1L);

        assertThat(commentRepository.findById(1L)).isEmpty();
    }

    @Test
    void deleteComment_whenUserIsBoardOwner_shouldDeleteOthersComment() {
        commentService.deleteComment(1L, 1L);

        assertThat(commentRepository.findById(1L)).isEmpty();
    }

    @Test
    @WithMockUser(id = 3L, username = "john_snow")
    void deleteComment_whenUserIsBoardEditor_shouldDeleteOthersComment() {
        commentService.deleteComment(1L, 1L);

        assertThat(commentRepository.findById(1L)).isEmpty();
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void deleteComment_whenUserCannotManageComment_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(id = 1L, username = "john_doe")
    void deleteComment_whenCommentNotFound_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> commentService.deleteComment(1L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
