package com.walking.backend.audit.listener;

import com.walking.backend.domain.event.UserActivityEvent;
import com.walking.backend.domain.event.UserActivityInternalEvent;
import com.walking.backend.domain.model.UserActivity;
import com.walking.backend.repository.UserActivityRepository;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActivityListener {
    private final UserActivityRepository userActivityRepository;
    private final KafkaProducerService kafkaProducerService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActivity(UserActivityInternalEvent userActivityInternalEvent) {
        UserActivity userActivity = UserActivity.builder()
                .userId(userActivityInternalEvent.userId())
                .username(userActivityInternalEvent.username())
                .boardId(userActivityInternalEvent.boardId())
                .boardName(userActivityInternalEvent.boardName())
                .activityType(userActivityInternalEvent.type())
                .description(userActivityInternalEvent.description())
                .build();

        UserActivity savedUserActivity = userActivityRepository.save(userActivity);

        UserActivityEvent event = UserActivityEvent.builder()
                .userId(userActivityInternalEvent.userId())
                .username(userActivityInternalEvent.username())
                .email(userActivityInternalEvent.email())
                .boardId(userActivityInternalEvent.boardId())
                .boardName(userActivityInternalEvent.boardName())
                .type(userActivityInternalEvent.type().name())
                .description(userActivityInternalEvent.description())
                .created(savedUserActivity.getCreated())
                .build();

        kafkaProducerService.sendUserActivityEvent(savedUserActivity.getId(), event);
    }
}
