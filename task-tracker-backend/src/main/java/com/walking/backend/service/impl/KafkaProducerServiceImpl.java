package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.event.UserActivityEvent;
import com.walking.backend.props.AppProperties;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<Long, Object> kafkaTemplate;
    private final AppProperties.Kafka kafkaProperties;

    @Override
    public void sendMessageDto(Long key, MessageDto messageDto) {
        kafkaTemplate.send(kafkaProperties.getTopics().getUserActivity(), key, messageDto)
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
        kafkaTemplate.send(kafkaProperties.getTopics().getUserActivity(), key, userActivityEvent)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send activity to Kafka: {}", userActivityEvent, exception);
                    } else {
                        log.debug("Activity sent to Kafka for user: {}", userActivityEvent.getUserId());
                    }
                });
    }
}
