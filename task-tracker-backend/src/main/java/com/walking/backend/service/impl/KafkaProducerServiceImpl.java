package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.event.UserActivityEvent;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<Long, Object> kafkaTemplate;

    @Value("${app.kafka.topics.email-sending}")
    private final String emailSendingTopic;

    @Value("${app.kafka.topics.user-activity}")
    private final String userActivityEventTopic;

    @Override
    public void sendMessageDto(Long key, MessageDto messageDto) {
        kafkaTemplate.send(emailSendingTopic, key, messageDto)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send message to Kafka: key={}", key, exception);
                    } else {
                        log.debug("Message sent to Kafka: key={}, partition={}", key, result.getRecordMetadata().partition());
                    }
                });
    }

    @Override
    public void sendUserActivityEvent(Long key, UserActivityEvent userActivityEvent) {
        kafkaTemplate.send(userActivityEventTopic, key, userActivityEvent)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send activity to Kafka: {}", userActivityEvent, exception);
                    } else {
                        log.debug("Activity sent to Kafka for user: {}", userActivityEvent.getUserId());
                    }
                });
    }
}
