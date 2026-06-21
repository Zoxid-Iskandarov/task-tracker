package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.comment.CommentRequest;
import com.walking.backend.domain.dto.comment.CommentResponse;
import com.walking.backend.domain.dto.user.UserShortResponse;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.Comment;
import com.walking.backend.repository.CommentRepository;
import com.walking.backend.service.CommentService;
import com.walking.backend.service.TaskService;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.comment.CommentRequestMapper;
import com.walking.backend.service.mapper.comment.CommentResponseMapper;
import com.walking.backend.util.UserMapLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final UserService userService;
    private final CommentRequestMapper commentRequestMapper;
    private final CommentResponseMapper commentResponseMapper;

    @Override
    @PreAuthorize("@resourceAccessService.canViewTask(#taskId, principal.id)")
    public Page<CommentResponse> getComments(Long taskId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findAllByTaskId(taskId, pageable);

        Map<Long, UserShortResponse> users = UserMapLoader
                .loadUserMap(comments.getContent(), Comment::getAuthor, userService);

        return comments.map(comment -> {
            UserShortResponse author = comment.getAuthor() != null
                    ? users.get(comment.getAuthor().getId())
                    : null;

            return commentResponseMapper.toDto(comment, author);
        });
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canViewTask(#taskId, #authorId)")
    public CommentResponse createComment(Long taskId, Long authorId, CommentRequest commentRequest) {
        Comment comment = commentRequestMapper.toEntity(commentRequest);
        comment.setTask(taskService.getProxyTaskById(taskId));
        comment.setAuthor(userService.getProxyUserById(authorId));

        Comment savedComment = commentRepository.save(comment);

        return commentResponseMapper.toDto(savedComment, userService.getUserShortById(authorId));
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canEditComment(#commentId, principal.id)")
    public CommentResponse updateComment(Long taskId, Long commentId, CommentRequest commentRequest) {
        Comment comment = commentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Comment with id %d not found".formatted(commentId)));

        comment.setContent(commentRequest.content());

        Comment updatedComment = commentRepository.save(comment);

        Long authorId = updatedComment.getAuthor() != null
                ? updatedComment.getAuthor().getId()
                : null;

        UserShortResponse author = authorId != null
                ? userService.getUserShortById(authorId)
                : null;

        return commentResponseMapper.toDto(updatedComment, author);
    }

    @Override
    @Transactional
    @PreAuthorize("@resourceAccessService.canManageComment(#commentId, #taskId, principal.id)")
    public void deleteComment(Long taskId, Long commentId) {
        Comment comment = commentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new ObjectNotFoundException("Comment with id %d not found".formatted(commentId)));

        commentRepository.delete(comment);
    }
}
