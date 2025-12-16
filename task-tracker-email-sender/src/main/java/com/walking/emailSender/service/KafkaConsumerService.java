package com.walking.emailSender.service;

import com.walking.emailSender.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final MailSenderService mailSenderService;

    @KafkaListener(topics = "${kafka.topic-name}")
    public void consumeMessage(MessageDto messageDto) {
        mailSenderService.sendMessage(messageDto);
    }
}
