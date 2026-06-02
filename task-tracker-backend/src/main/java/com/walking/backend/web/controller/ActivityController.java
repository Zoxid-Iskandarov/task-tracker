package com.walking.backend.web.controller;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.security.principal.CustomUserDetails;
import com.walking.backend.service.UserActivityService;
import com.walking.backend.web.openapi.ActivityApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActivityController implements ActivityApi {
    private final UserActivityService userActivityService;

    @GetMapping("/users/me/activities")
    public Page<UserActivityResponse> getUserActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 50, sort = "created", direction = Sort.Direction.DESC) Pageable pageable) {
        return userActivityService.getUserActivities(userDetails.id(), pageable);
    }

    @GetMapping("/boards/{boardId}/activities")
    public Page<BoardActivityResponse> getBoardActivities(
            @PathVariable Long boardId,
            @PageableDefault(size = 50, sort = "created", direction = Sort.Direction.DESC) Pageable pageable) {
        return userActivityService.getBoardActivities(boardId, pageable);
    }
}
