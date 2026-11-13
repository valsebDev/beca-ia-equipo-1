package com.ids.ms_users.dto.response;

import com.ids.ms_users.domain.Role;
import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    Role role,
    Boolean active,
    Instant createdAt
) {}
