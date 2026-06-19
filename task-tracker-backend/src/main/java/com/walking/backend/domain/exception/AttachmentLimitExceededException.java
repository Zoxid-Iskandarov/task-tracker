package com.walking.backend.domain.exception;

public class AttachmentLimitExceededException extends RuntimeException {
    public AttachmentLimitExceededException(String message) {
        super(message);
    }
}
