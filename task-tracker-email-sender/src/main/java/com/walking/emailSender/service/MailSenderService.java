package com.walking.emailSender.service;

import com.walking.emailSender.dto.MessageDto;

public interface MailSenderService {

    void sendMessage(MessageDto messageDto);
}
