package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.domain.User;
import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user
) {
    public record UserResponse(UUID id, UUID tenantId, String email, String fullName, String role) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getTenantId(), u.getEmail(),
                    u.getFullName(), u.getRole().name());
        }
    }

    public static LoginResponse from(String accessToken, long expiresInSeconds, User user) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, UserResponse.from(user));
    }
}
