package com.SaaS.Constructora.identity.domain;

public enum Role {
    ADMIN,
    MEMBER,
    WORKER;

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

    /**
     * Los ADMIN siempre pueden consultar candidatos a asignación. Un MEMBER
     * puede hacerlo solo si un ADMIN le concedió el permiso explícito en su
     * membresía (ver {@link Membership#canViewAssignmentCandidates()}).
     */
    public boolean canViewAssignmentCandidates() {
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
     * WORKER nunca puede crear personal.
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

    /**
     * Los ADMIN siempre pueden crear tareas. WORKER y MEMBER requieren que un
     * ADMIN les conceda el permiso puntual en su membresía
     * (ver {@link Membership#canCreateTasks()}).
     */
    public boolean canCreateTasks() {
        return isAdmin();
    }

    /**
     * Los ADMIN siempre pueden editar o eliminar entradas de la bitácora. Un
     * WORKER o MEMBER solo si un ADMIN le concedió el permiso puntual en su
     * membresía (ver {@link Membership#canManageSiteLog()}).
     */
    public boolean canManageSiteLog() {
        return isAdmin();
    }
}
