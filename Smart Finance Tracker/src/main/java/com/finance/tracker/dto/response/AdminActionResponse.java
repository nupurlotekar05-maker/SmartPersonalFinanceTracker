package com.finance.tracker.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionResponse {
    private Long id;
    private String type;
    private String requestedBy;
    private String approvedBy;
    private String status;
    private String metadata;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
