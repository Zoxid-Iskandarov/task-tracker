package com.walking.backend.domain.exception;

public class InvalidTaskAssigneeException extends RuntimeException {
    public InvalidTaskAssigneeException(String message) {
        super(message);
    }
}
