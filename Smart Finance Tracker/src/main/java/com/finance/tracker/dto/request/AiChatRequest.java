package com.finance.tracker.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AiChatRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;

    public AiChatRequest() {}

    public AiChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
