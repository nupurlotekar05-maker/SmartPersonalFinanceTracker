package com.finance.tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastRequest {
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String type; // INFO, WARNING, SUCCESS, ERROR
}
