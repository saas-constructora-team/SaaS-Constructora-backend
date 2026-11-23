package com.SaaS.Constructora.identity.application;

/** Datos de entrada del caso de uso de registro de empresa y administrador. */
public record RegisterTenantCommand(
        String companyName,
        String tradeName,
        String taxId,
        String contactPhone,
        String address,
        String companyEmail,
        String adminFullName,
        String adminEmail,
        String adminPassword
) { }
