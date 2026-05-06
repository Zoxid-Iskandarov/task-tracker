package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.kafka.MessageDto;
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
    private final KafkaTemplate<Long, MessageDto> kafkaTemplate;

    @Value("${app.kafka.topic-name}")
    private final String topic;

    @Override
    public void sendMessageDto(Long key, MessageDto messageDto) {
        kafkaTemplate.send(topic, key, messageDto)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send message to Kafka: key={}", key, exception);
                    }

                    log.info("Message sent to Kafka: key={}, partition={}", key, result.getRecordMetadata().partition());
                });
    }
}
