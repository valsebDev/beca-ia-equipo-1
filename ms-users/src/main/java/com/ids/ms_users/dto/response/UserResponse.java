package com.ids.ms_users.dto.response;

import com.ids.ms_users.domain.Role;
import java.time.LocalDateTime;

public record UserResponse(
    String userIdentifier,
    String name,
    String paternalName,
    String maternalName,
    String email,
    Role role,
    Boolean isActive,
    LocalDateTime createdAt
) {}
