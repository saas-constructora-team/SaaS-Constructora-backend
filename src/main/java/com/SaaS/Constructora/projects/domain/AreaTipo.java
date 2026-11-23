package com.SaaS.Constructora.projects.domain;

/**
 * Tipo de un área de trabajo. Define qué campos específicos se cargan en las
 * tareas del área. Se asigna al crear el área y no cambia aunque se renombre.
 */
public enum AreaTipo {
    OBRA_INGENIERIA,
    COMPRAS_ALMACEN,
    DISENO_ARQUITECTURA,
    LEGAL_TRAMITES,
    CONTABILIDAD_FINANZAS,
    PERSONALIZADA
}
