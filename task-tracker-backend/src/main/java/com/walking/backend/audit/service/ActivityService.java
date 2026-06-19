package com.walking.backend.audit.service;

import com.walking.backend.domain.event.UserActivityInternalEvent;
import com.walking.backend.domain.model.ActivityType;
import com.walking.backend.domain.model.Board;
import com.walking.backend.security.principal.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ApplicationEventPublisher eventPublisher;

    public void publish(Board board, ActivityType type, String description) {
        CustomUserDetails userDetails = getCurrentUser();

        eventPublisher.publishEvent(new UserActivityInternalEvent(
                userDetails.id(),
                userDetails.username(),
                userDetails.email(),
                board.getId(),
                board.getName(),
                type,
                description));
    }

    private CustomUserDetails getCurrentUser() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
