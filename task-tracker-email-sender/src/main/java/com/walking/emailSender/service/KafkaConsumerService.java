package com.walking.emailSender.service;

import com.walking.emailSender.dto.MessageDto;

public interface KafkaConsumerService {

    void consumeMessage(MessageDto messageDto);
}
