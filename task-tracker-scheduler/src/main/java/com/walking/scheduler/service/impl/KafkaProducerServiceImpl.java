package com.walking.scheduler.service.impl;

import com.walking.scheduler.domain.dto.MessageDto;
import com.walking.scheduler.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<String, MessageDto> kafkaTemplate;

    @Value("${kafka.topic-name}")
    private String topic;

    @Override
    public void sendMessage(String key, MessageDto messageDto) {
        kafkaTemplate.send(topic, key, messageDto);
    }
}
