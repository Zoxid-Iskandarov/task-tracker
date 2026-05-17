package com.walking.scheduler.service;

import com.walking.scheduler.domain.dto.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final UserActivityService userActivityService;

    @KafkaListener(topics = "${app.kafka.topics.user_activity}")
    public void consumeMessage(UserActivityEvent activityEvent) {
        log.debug("Receive userActivity: userId={}, activityType={}", activityEvent.getUserId(), activityEvent.getType());

        try {
            userActivityService.saveUserActivity(activityEvent);
            log.debug("UserActivity successfully saved");
        } catch (Exception e) {
            log.error("UserActivity save failed", e);
            throw e;
        }
    }
}
