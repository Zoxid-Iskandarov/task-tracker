package com.walking.backend.service;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.dto.activity.UserActivityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserActivityService {

    Page<BoardActivityResponse> getBoardActivities(Long boardId, Pageable pageable);

    Page<UserActivityResponse> getUserActivities(Long userId, Pageable pageable);
}
