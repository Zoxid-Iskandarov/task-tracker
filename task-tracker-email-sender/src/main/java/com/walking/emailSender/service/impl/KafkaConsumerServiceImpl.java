package com.walking.emailSender.service.impl;

import com.walking.emailSender.dto.MessageDto;
import com.walking.emailSender.service.KafkaConsumerService;
import com.walking.emailSender.service.MailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerServiceImpl implements KafkaConsumerService {
    private final MailSenderService mailSenderService;

    @Override
    @KafkaListener(topics = "${app.kafka.topic-name}")
    public void consumeMessage(MessageDto messageDto) {
        log.debug("Received email task for: {}", messageDto.getEmail());

        try {
            mailSenderService.sendMessage(messageDto);
            log.debug("Email successfully sent to: {}", messageDto.getEmail());
        } catch (Exception e) {
            log.error("Error processing email task for: {}", messageDto.getEmail(), e);
            throw e;
        }
    }
}
