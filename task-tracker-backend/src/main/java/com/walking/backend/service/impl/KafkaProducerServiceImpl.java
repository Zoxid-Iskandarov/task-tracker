package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<String, MessageDto> kafkaTemplate;

    @Value("${kafka.topic-name}")
    private final String topic;

    @Override
    public void sendMessageDto(String key, MessageDto messageDto) {
        kafkaTemplate.send(topic, key, messageDto);
    }
}
