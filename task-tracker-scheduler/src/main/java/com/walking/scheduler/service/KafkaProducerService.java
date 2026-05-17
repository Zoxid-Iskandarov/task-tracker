package com.walking.scheduler.service;

import com.walking.scheduler.domain.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<Long, Object> kafkaTemplate;

    @Value("${app.kafka.topics.email-sending}")
    private final String emailSendingTopic;

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
}
