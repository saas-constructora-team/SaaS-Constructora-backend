package com.SaaS.Constructora.shared.validation;

/**
 * Reglas de complejidad de contraseña compartidas entre la validación estructural
 * (anotaciones Jakarta en los DTOs) y la validación contextual (casos de uso).
 */
public final class PasswordPolicy {

    /** Mínimo 8 caracteres, al menos una letra, un número y un símbolo. */
    public static final String PATTERN = "(?s)^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    public static final String MESSAGE =
            "La contraseña debe tener al menos 8 caracteres e incluir letras, números y símbolos";

    private PasswordPolicy() {
    }

    /** Comprueba la complejidad de la contraseña de forma programática. */
    public static boolean isValid(String password) {
        return password != null && password.matches(PATTERN);
    }
}
