package com.ids.ms_users.dto.response;

import com.ids.ms_users.domain.Role;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    Role role,
    Boolean active,
    LocalDateTime createdAt
) {}
