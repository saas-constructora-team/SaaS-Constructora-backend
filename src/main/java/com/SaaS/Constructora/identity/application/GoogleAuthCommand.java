package com.SaaS.Constructora.identity.application;

/** Datos de entrada del caso de uso de autenticación con Google. */
public record GoogleAuthCommand(String idToken, String companyName) { }
