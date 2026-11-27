package com.SaaS.Constructora.sitelog.interfaces;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssociateDocumentRequest(@NotNull(message = "El documento es obligatorio") UUID documentId) { }
