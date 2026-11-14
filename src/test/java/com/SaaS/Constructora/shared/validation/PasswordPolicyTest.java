package com.SaaS.Constructora.shared.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Secreta123!",
            "abc123!@",
            "MiClave.2026",
            "Pass word 9#"
    })
    void contraseniasQueCumplenLaPoliticaSonValidas(String password) {
        assertTrue(PasswordPolicy.isValid(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "corta",
            "SinSimbolo123",
            "SinNumero!abc",
            "12345678!",
            "SoloLetrasABC",
            "        "
    })
    void contraseniasQueNoCumplenLaPoliticaSonInvalidas(String password) {
        assertFalse(PasswordPolicy.isValid(password));
    }

    @Test
    void contrseniaNulaNoEsValida() {
        assertFalse(PasswordPolicy.isValid(null));
    }
}
