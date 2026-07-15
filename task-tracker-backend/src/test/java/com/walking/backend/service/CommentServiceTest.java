package com.walking.backend.service;

import com.walking.backend.domain.dto.comment.CommentRequest;
import com.walking.backend.domain.dto.comment.CommentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Comment;
import com.walking.backend.domain.model.Task;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.CommentRepository;
import com.walking.backend.service.impl.CommentServiceImpl;
import com.walking.backend.service.mapper.comment.CommentRequestMapper;
import com.walking.backend.service.mapper.comment.CommentResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    private static final Long COMMENT_ID = 1L;
    private static final Long TASK_ID = 2L;
    private static final Long USER_ID = 3L;
    private static final String USER_USERNAME = "Dante";

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    @Mock
    private CommentRequestMapper commentRequestMapper;

    @Mock
    private CommentResponseMapper commentResponseMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void getComments_whenCommentHasAuthor_shouldReturnPageOfCommentResponses() {
        var pageable = PageRequest.of(0, 10);
        var author = buildUser(USER_ID, USER_USERNAME);
        var comment = buildComment(COMMENT_ID, author);
        var commentsPage = new PageImpl<>(List.of(comment), pageable, 1L);

        var userShortResponse = buildUserShortResponse(author);
        var commentResponse = buildCommentResponse(userShortResponse, comment);

        doReturn(commentsPage).when(commentRepository).findAllByTaskId(TASK_ID, pageable);
        doReturn(List.of(userShortResponse)).when(userService).getUserShortsByIds(Set.of(USER_ID));
        doReturn(commentResponse).when(commentResponseMapper).toDto(comment, userShortResponse);

        Page<CommentResponse> actual = commentService.getComments(TASK_ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(1, actual.getContent().size());
        assertEquals(commentResponse, actual.getContent().getFirst());

        verify(commentRepository).findAllByTaskId(TASK_ID, pageable);
        verify(userService).getUserShortsByIds(Set.of(USER_ID));
        verify(commentResponseMapper).toDto(comment, userShortResponse);
    }

    @Test
    void getComments_whenCommentHasNoAuthor_shouldReturnCommentResponseWithNullAuthorAndSkipUserServiceCall() {
        var pageable = PageRequest.of(0, 10);
        var comment = buildComment(COMMENT_ID, null);
        var commentsPage = new PageImpl<>(List.of(comment), pageable, 1L);

        var commentResponse = buildCommentResponse(null, comment);

        doReturn(commentsPage).when(commentRepository).findAllByTaskId(TASK_ID, pageable);
        doReturn(commentResponse).when(commentResponseMapper).toDto(comment, null);

        Page<CommentResponse> actual = commentService.getComments(TASK_ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(1, actual.getContent().size());
        assertEquals(commentResponse, actual.getContent().getFirst());

        verify(commentRepository).findAllByTaskId(TASK_ID, pageable);
        verify(userService, never()).getUserShortsByIds(anySet());
        verify(commentResponseMapper).toDto(comment, null);
    }

    @Test
    void getComments_whenMultipleCommentsWithDifferentAuthors_shouldMapEachCommentToCorrectAuthor() {
        var pageable = PageRequest.of(0, 10);
        var user1 = buildUser(1L, "Vergil");
        var user2 = buildUser(2L, "Anakin");
        var comment1 = buildComment(1L, user1);
        var comment2 = buildComment(2L, user2);
        var commentsPage = new PageImpl<>(List.of(comment1, comment2), pageable, 2L);

        var userShort1 = buildUserShortResponse(user1);
        var userShort2 = buildUserShortResponse(user2);

        var commentResponse1 = buildCommentResponse(userShort1, comment1);
        var commentResponse2 = buildCommentResponse(userShort2, comment2);

        doReturn(commentsPage).when(commentRepository).findAllByTaskId(TASK_ID, pageable);
        doReturn(List.of(userShort1, userShort2)).when(userService).getUserShortsByIds(Set.of(user1.getId(), user2.getId()));
        doReturn(commentResponse1).when(commentResponseMapper).toDto(comment1, userShort1);
        doReturn(commentResponse2).when(commentResponseMapper).toDto(comment2, userShort2);

        Page<CommentResponse> actual = commentService.getComments(TASK_ID, pageable);

        assertTrue(actual.hasContent());
        assertEquals(2, actual.getContent().size());
        assertEquals(List.of(commentResponse1, commentResponse2), actual.getContent());

        verify(commentRepository).findAllByTaskId(TASK_ID, pageable);
        verify(userService).getUserShortsByIds(Set.of(user1.getId(), user2.getId()));
        verify(commentResponseMapper).toDto(comment1, userShort1);
        verify(commentResponseMapper).toDto(comment2, userShort2);
    }

    @Test
    void createComment_whenValidRequestData_shouldCreateAndReturnCommentResponse() {
        var commentRequest = new CommentRequest("Comment content");
        var user = buildUser(USER_ID, USER_USERNAME);
        var task = new Task();
        var comment = buildComment(COMMENT_ID, user, commentRequest.content());
        var userShortResponse = buildUserShortResponse(user);
        var commentResponse = buildCommentResponse(userShortResponse, comment);

        doReturn(comment).when(commentRequestMapper).toEntity(commentRequest);
        doReturn(task).when(taskService).getProxyTaskById(TASK_ID);
        doReturn(user).when(userService).getProxyUserById(USER_ID);
        doReturn(comment).when(commentRepository).save(comment);
        doReturn(userShortResponse).when(userService).getUserShortById(user.getId());
        doReturn(commentResponse).when(commentResponseMapper).toDto(comment, userShortResponse);

        CommentResponse actual = commentService.createComment(TASK_ID, user.getId(), commentRequest);

        assertEquals(commentResponse, actual);
        assertEquals(task, comment.getTask());
        assertEquals(user, comment.getAuthor());

        verify(commentRequestMapper).toEntity(commentRequest);
        verify(taskService).getProxyTaskById(TASK_ID);
        verify(userService).getProxyUserById(user.getId());
        verify(commentRepository).save(comment);
        verify(userService).getUserShortById(user.getId());
        verify(commentResponseMapper).toDto(comment, userShortResponse);
    }

    @Test
    void updateComment_whenCommentNotFound_shouldThrowObjectNotFoundException() {
        var commentRequest = new CommentRequest("Comment content");

        doReturn(Optional.empty()).when(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> commentService.updateComment(TASK_ID, COMMENT_ID, commentRequest));

        verify(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);
        verify(commentRepository, never()).save(any(Comment.class));
        verifyNoInteractions(userService, commentResponseMapper);
    }

    @Test
    void updateComment_whenValidRequestData_shouldUpdateCommentAndReturnCommentResponse() {
        var commentRequest = new CommentRequest("Comment content");
        var author = buildUser(USER_ID, USER_USERNAME);
        var comment = buildComment(COMMENT_ID, author, commentRequest.content());
        var userShortResponse = buildUserShortResponse(author);
        var commentResponse = buildCommentResponse(userShortResponse, comment);

        doReturn(Optional.of(comment)).when(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);
        doReturn(comment).when(commentRepository).save(comment);
        doReturn(userShortResponse).when(userService).getUserShortById(author.getId());
        doReturn(commentResponse).when(commentResponseMapper).toDto(comment, userShortResponse);

        CommentResponse actual = commentService.updateComment(TASK_ID, COMMENT_ID, commentRequest);

        assertEquals(commentResponse, actual);
        assertEquals(userShortResponse, actual.author());
        assertEquals(commentRequest.content(), actual.content());

        verify(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);
        verify(commentRepository).save(comment);
        verify(userService).getUserShortById(author.getId());
        verify(commentResponseMapper).toDto(comment, userShortResponse);
    }

    @Test
    void deleteComment_whenCommentNotFound_shouldThrowObjectNotFoundException() {
        doReturn(Optional.empty()).when(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);

        assertThrows(ObjectNotFoundException.class, () -> commentService.deleteComment(TASK_ID, COMMENT_ID));

        verify(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteComment_whenValidRequestData_shouldDeleteComment() {
        Comment comment = buildComment(COMMENT_ID, buildUser(USER_ID, USER_USERNAME));

        doReturn(Optional.of(comment)).when(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);

        commentService.deleteComment(TASK_ID, COMMENT_ID);

        verify(commentRepository).findByIdAndTaskId(COMMENT_ID, TASK_ID);
        verify(commentRepository).delete(comment);
    }
    
    private Comment buildComment(Long commentId, User author) {
        return buildComment(commentId, author, "Madmen blaze the trails that the sensible will follow.");
    }

    private Comment buildComment(Long commentId, User author, String content) {
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setContent(content);
        comment.setAuthor(author);

        return comment;
    }

    private User buildUser(Long userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        
        return user;
    }

    private UserShortResponse buildUserShortResponse(User user) {
        return new UserShortResponse(user.getId(), user.getUsername(), "", "");
    }

    private CommentResponse buildCommentResponse(UserShortResponse user, Comment comment) {
        return new CommentResponse(
                comment.getId(), comment.getContent(), user, false, LocalDateTime.now(), LocalDateTime.now());
    }
}
