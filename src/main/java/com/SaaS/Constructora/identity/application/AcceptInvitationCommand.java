package com.SaaS.Constructora.identity.application;

/** Datos de entrada del caso de uso para aceptar una invitación. */
public record AcceptInvitationCommand(String token, String password) { }
