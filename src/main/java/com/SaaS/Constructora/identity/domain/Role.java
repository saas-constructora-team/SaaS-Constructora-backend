package com.SaaS.Constructora.identity.domain;

public enum Role {
    ADMIN,
    MEMBER;

    /**
     * Por ahora solo ADMIN ve todos los proyectos de la empresa.
     * Cuando se definan roles superiores, esta regla se amplía aquí.
     */
    public boolean canViewAllProjects() {
        return this == ADMIN;
    }

    /** Solo ADMIN puede gestionar asignaciones de proyectos. */
    public boolean canManageAssignments() {
        return this == ADMIN;
    }
}
