package com.finance.tracker.exception;

public class ApiKeyMissingException extends RuntimeException {
    public ApiKeyMissingException(String message) {
        super(message);
    }
}
