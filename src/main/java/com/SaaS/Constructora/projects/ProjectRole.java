package com.SaaS.Constructora.projects;

/**
 * Rol de un usuario dentro de un proyecto concreto.
 * <p>
 * Es independiente del {@link com.SaaS.Constructora.identity.domain.Role} del
 * tenant: un MEMBER a nivel empresa puede ser LEAD en un proyecto si un ADMIN
 * se lo delega explícitamente.
 */
public enum ProjectRole {

    /** Miembro regular: accede y edita el proyecto, pero no gestiona sus miembros. */
    MEMBER,

    /** Líder delegado por un ADMIN: puede agregar, editar y quitar miembros del proyecto. */
    LEAD;

    /** Solo el líder del proyecto puede gestionar a los demás miembros. */
    public boolean canManageMembers() {
        return this == LEAD;
    }
}
