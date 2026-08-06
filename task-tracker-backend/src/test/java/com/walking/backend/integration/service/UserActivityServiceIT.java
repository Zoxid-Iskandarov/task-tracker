package com.walking.backend.integration.service;

import com.walking.backend.domain.dto.activity.BoardActivityResponse;
import com.walking.backend.domain.dto.activity.UserActivityResponse;
import com.walking.backend.integration.IntegrationTestBase;
import com.walking.backend.integration.annotation.WithMockUser;
import com.walking.backend.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static com.walking.backend.domain.model.ActivityType.BOARD_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser
@RequiredArgsConstructor
public class UserActivityServiceIT extends IntegrationTestBase {
    private final UserActivityService userActivityService;

    @Test
    void getBoardActivities_whenUserHasAccess_shouldReturnActivitiesPage() {
        var pageable = PageRequest.of(0, 10);

        Page<BoardActivityResponse> actual = userActivityService.getBoardActivities(1L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(BoardActivityResponse::activityType)
                .contains(BOARD_CREATED);
    }

    @Test
    @WithMockUser(id = 99L, username = "stranger")
    void getBoardActivities_whenUserHasNoAccess_shouldThrowAccessDeniedException() {
        var pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> userActivityService.getBoardActivities(1L, pageable))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getUserActivities_whenUserHasActivities_shouldReturnActivitiesPage() {
        var pageable = PageRequest.of(0, 10);

        Page<UserActivityResponse> actual = userActivityService.getUserActivities(2L, pageable);

        assertThat(actual.getContent()).isNotEmpty();
        assertThat(actual.getContent())
                .extracting(UserActivityResponse::activityType)
                .contains(BOARD_CREATED);
    }

    @Test
    void getUserActivities_whenUserHasNoActivities_shouldReturnEmptyPage() {
        var pageable = PageRequest.of(0, 10);

        Page<UserActivityResponse> actual = userActivityService.getUserActivities(99L, pageable);

        assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getTotalElements()).isZero();
    }
}
