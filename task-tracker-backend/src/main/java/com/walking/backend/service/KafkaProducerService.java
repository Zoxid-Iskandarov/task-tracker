package com.walking.backend.service;

import com.walking.backend.domain.dto.kafka.MessageDto;
import com.walking.backend.domain.event.UserActivityEvent;

public interface KafkaProducerService {

    void sendMessageDto(Long key, MessageDto messageDto);

    void sendUserActivityEvent(Long key, UserActivityEvent userActivityEvent);
}
