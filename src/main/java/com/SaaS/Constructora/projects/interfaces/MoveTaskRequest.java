package com.SaaS.Constructora.projects.interfaces;

import java.util.UUID;

/** Etapa destino de una tarea; `null` la deja en la columna «Sin etapa». */
public record MoveTaskRequest(UUID stageId) { }
