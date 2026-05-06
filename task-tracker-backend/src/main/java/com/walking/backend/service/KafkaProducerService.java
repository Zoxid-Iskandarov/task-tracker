package com.walking.backend.service;

import com.walking.backend.domain.dto.kafka.MessageDto;

public interface KafkaProducerService {

    void sendMessageDto(Long key, MessageDto messageDto);
}
