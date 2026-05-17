package com.walking.scheduler.service;

import com.walking.scheduler.domain.dto.UserActivityEvent;
import com.walking.scheduler.service.mapper.UserActivityEventMapper;
import com.walking.scheduler.domain.model.UserActivity;
import com.walking.scheduler.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActivityService {
    private final UserActivityRepository userActivityRepository;
    private final UserActivityEventMapper userActivityEventMapper;

    @Transactional
    public void saveUserActivity(UserActivityEvent userActivityEvent) {
        UserActivity userActivity = userActivityEventMapper.toEntity(userActivityEvent);
        userActivityRepository.save(userActivity);
    }
}
