package com.walking.backend.service;

import com.walking.backend.domain.dto.comment.CommentRequest;
import com.walking.backend.domain.dto.comment.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Page<CommentResponse> getComments(Long taskId, Pageable pageable);

    CommentResponse createComment(Long taskId, Long authorId, CommentRequest commentRequest);

    CommentResponse updateComment(Long taskId, Long commentId, CommentRequest commentRequest);

    void deleteComment(Long taskId, Long commentId);
}
