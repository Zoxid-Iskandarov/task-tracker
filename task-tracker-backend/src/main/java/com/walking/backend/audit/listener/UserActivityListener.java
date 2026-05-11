package com.walking.backend.audit.listener;

import com.walking.backend.domain.dto.kafka.UserActivityEvent;
import com.walking.backend.domain.model.UserActivity;
import com.walking.backend.repository.UserActivityRepository;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActivity(UserActivity userActivity) {
        UserActivity savedUserActivity = userActivityRepository.save(userActivity);

        UserActivityEvent event = UserActivityEvent.builder()
                .userId(savedUserActivity.getUserId())
                .username(savedUserActivity.getUsername())
                .email(savedUserActivity.getEmail())
                .boardId(savedUserActivity.getBoardId())
                .boardName(savedUserActivity.getBoardName())
                .type(savedUserActivity.getActivityType().name())
                .description(savedUserActivity.getDescription())
                .created(savedUserActivity.getCreated())
                .build();

        kafkaProducerService.sendUserActivityEvent(savedUserActivity.getId(), event);
    }
}
