package com.finance.tracker.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String action;
    private String description;
    private String ipAddress;
    private String logType;
    private LocalDateTime createdAt;
}
