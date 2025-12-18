package com.walking.emailSender.service.impl;

import com.walking.emailSender.dto.MessageDto;
import com.walking.emailSender.service.KafkaConsumerService;
import com.walking.emailSender.service.MailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerServiceImpl implements KafkaConsumerService {
    private final MailSenderService mailSenderService;

    @Override
    @KafkaListener(topics = "${kafka.topic-name}")
    public void consumeMessage(MessageDto messageDto) {
        mailSenderService.sendMessage(messageDto);
    }
}
