package com.finance.tracker.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    private String name;

    @Email
    private String email;

    private String status; // ACTIVE, BLOCKED

    private String role;   // USER, ADMIN

    private Boolean isVerified;
}
