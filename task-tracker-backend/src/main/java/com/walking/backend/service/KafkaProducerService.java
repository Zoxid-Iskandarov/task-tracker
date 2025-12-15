package com.walking.backend.service;

import com.walking.backend.domain.dto.kafka.MessageDto;

public interface KafkaProducerService {

    void sendMessageDto(String key, MessageDto messageDto);
}
