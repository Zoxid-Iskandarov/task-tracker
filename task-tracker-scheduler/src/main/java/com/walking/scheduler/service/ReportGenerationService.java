package com.walking.scheduler.service;

import com.walking.scheduler.domain.dto.MessageDto;
import com.walking.scheduler.domain.model.UserActivity;
import com.walking.scheduler.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {
    private final UserActivityRepository userActivityRepository;
    private final MessageService messageService;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public boolean processBatch(int batchSize) {
        List<String> emails = userActivityRepository.findUnprocessedEmails(batchSize);

        if (emails.isEmpty()) {
            return false;
        }

        List<UserActivity> unprocessedActivities = userActivityRepository.findAllByEmailInAndIsProcessedFalse(emails);

        Map<String, List<UserActivity>> activitiesByEmail = unprocessedActivities.stream()
                .collect(Collectors.groupingBy(UserActivity::getEmail));

        for (Map.Entry<String, List<UserActivity>> entry : activitiesByEmail.entrySet()) {
            String email = entry.getKey();
            List<UserActivity> userActivities = entry.getValue();

            Long userId = userActivities.getFirst().getUserId();
            String username = userActivities.getFirst().getUsername();

            MessageDto messageDto = new MessageDto(
                    email,
                    "Your Daily Activity Report",
                    messageService.generateMessage(username, userActivities));
            kafkaProducerService.sendMessageDto(userId, messageDto);
        }

        List<Long> activityIds = unprocessedActivities.stream()
                .map(UserActivity::getId)
                .toList();

        userActivityRepository.markAsProcessed(activityIds);

        return true;
    }
}
