package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.domain.StageColor;

import java.util.List;
import java.util.UUID;

/**
 * Lectura única del tablero de tareas: etapas de los proyectos visibles y
 * tareas con su etapa, sin obligar al cliente a consultar proyecto por proyecto.
 */
public record TaskBoardResponse(
        List<Stage> stages,
        List<TaskResponse> tasks,
        boolean truncated) {

    /** `closed` permite deshabilitar altas y movimientos en etapas ya cerradas. */
    public record Stage(UUID id, UUID projectId, String name, int order,
                        StageColor color, boolean closed) { }
}
