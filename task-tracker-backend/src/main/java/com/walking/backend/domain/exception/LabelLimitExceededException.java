package com.walking.backend.domain.exception;

public class LabelLimitExceededException extends RuntimeException {
    public LabelLimitExceededException(String message) {
        super(message);
    }
}
