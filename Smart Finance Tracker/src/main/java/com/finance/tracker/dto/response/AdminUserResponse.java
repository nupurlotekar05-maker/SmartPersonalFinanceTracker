package com.finance.tracker.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;
    private Boolean isVerified;
    private Boolean accountLocked;
    private Integer failedAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String suspensionReason;
}
