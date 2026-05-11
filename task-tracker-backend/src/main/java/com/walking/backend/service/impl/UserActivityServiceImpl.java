package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.repository.UserActivityRepository;
import com.walking.backend.service.UserActivityService;
import com.walking.backend.service.mapper.activity.BoardActivityResponseMapper;
import com.walking.backend.service.mapper.activity.UserActivityResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {
    private final UserActivityRepository userActivityRepository;
    private final BoardActivityResponseMapper boardActivityResponseMapper;
    private final UserActivityResponseMapper userActivityResponseMapper;

    @Override
    @PreAuthorize("@resourceAccessService.canViewBoard(#boardId, principal.id)")
    public Page<BoardActivityResponse> getBoardActivities(Long boardId, Pageable pageable) {
        return userActivityRepository.findAllByBoardIdOrderByCreatedDesc(boardId, pageable)
                .map(boardActivityResponseMapper::toDto);
    }

    @Override
    public Page<UserActivityResponse> getUserActivities(Long userId, Pageable pageable) {
        return userActivityRepository.findAllByUserIdOrderByCreatedDesc(userId, pageable)
                .map(userActivityResponseMapper::toDto);
    }
}
