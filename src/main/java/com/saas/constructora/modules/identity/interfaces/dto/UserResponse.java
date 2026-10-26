package com.saas.constructora.modules.identity.interfaces.dto;

import com.saas.constructora.modules.identity.application.UserSummary;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String role,
        boolean active
) {
    public static UserResponse from(UserSummary user) {
        return new UserResponse(user.id(), user.email(), user.fullName(),
                user.role().name(), user.active());
    }
}
