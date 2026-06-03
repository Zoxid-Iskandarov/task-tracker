package com.walking.backend.domain.exception;

public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
