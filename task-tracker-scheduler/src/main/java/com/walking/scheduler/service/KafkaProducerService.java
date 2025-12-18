package com.walking.scheduler.service;

import com.walking.scheduler.domain.dto.MessageDto;

public interface KafkaProducerService {

    void sendMessage(String key, MessageDto messageDto);
}
