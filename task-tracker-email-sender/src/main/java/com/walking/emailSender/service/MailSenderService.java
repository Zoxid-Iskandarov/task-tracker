package com.walking.emailSender.service;

import com.walking.emailSender.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendMessage(MessageDto messageDto) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(from);
        mailMessage.setTo(messageDto.getEmail());
        mailMessage.setSubject(messageDto.getTitle());
        mailMessage.setText(messageDto.getMessage());

        javaMailSender.send(mailMessage);
    }
}
