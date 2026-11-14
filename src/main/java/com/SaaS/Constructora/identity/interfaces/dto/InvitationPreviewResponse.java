package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.application.InvitationPreview;

import java.time.LocalDateTime;

public record InvitationPreviewResponse(
        String companyName,
        String email,
        LocalDateTime expiresAt,
        boolean requiresPassword
) {
    public static InvitationPreviewResponse from(InvitationPreview preview) {
        return new InvitationPreviewResponse(preview.companyName(), preview.email(),
                preview.expiresAt(), preview.requiresPassword());
    }
}
