package com.SaaS.Constructora.identity.domain;

public enum Role {
    ADMIN,
    MEMBER;

    /** Rol con permisos de administración de la empresa. */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Por ahora solo ADMIN ve todos los proyectos de la empresa.
     * Cuando se definan roles superiores, esta regla se amplía aquí.
     */
    public boolean canViewAllProjects() {
        return isAdmin();
    }

    /** Solo ADMIN puede gestionar asignaciones de proyectos. */
    public boolean canManageAssignments() {
        return isAdmin();
    }

    /** Solo ADMIN puede crear proyectos dentro de su empresa. */
    public boolean canCreateProject() {
        return isAdmin();
    }

    /**
     * Los ADMIN siempre pueden crear personal. Un MEMBER puede hacerlo solo
     * si un ADMIN le concedió el permiso explícito en su membresía
     * (ver {@link Membership#canCreatePersonnel()}).
     */
    public boolean canCreatePersonnel() {
        return isAdmin();
    }

    /**
     * Solo ADMIN puede eliminar proyectos. Los MEMBER asignados pueden
     * consultarlos y editarlos, pero no borrarlos.
     */
    public boolean canDeleteProject() {
        return isAdmin();
    }
}
