package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.Role;

import java.util.UUID;

/** Proyección plana para listar usuarios activos sin consultas por fila. */
interface ActiveUserProjection {

    UUID getId();

    String getEmail();

    String getFullName();

    String getUsername();

    Role getRole();

    boolean isActive();

    boolean getCanCreatePersonnel();

    boolean getCanViewAssignmentCandidates();

    boolean getCanCreateTasks();

    boolean getCanManageSiteLog();
}
