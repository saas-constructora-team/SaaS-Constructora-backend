package com.SaaS.Constructora.identity.application;

/** Datos de entrada del caso de uso de login, independientes del DTO HTTP. */
public record LoginCommand(String email, String password) { }
