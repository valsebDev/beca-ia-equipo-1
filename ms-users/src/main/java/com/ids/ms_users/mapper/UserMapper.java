package com.ids.ms_users.mapper;

import com.ids.ms_users.domain.User;
import com.ids.ms_users.dto.response.UserResponse;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserIdentifier(),
                user.getName(),
                user.getPaternalName(),
                user.getMaternalName(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
