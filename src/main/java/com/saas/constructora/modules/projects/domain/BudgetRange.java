package com.saas.constructora.modules.projects.domain;

/** Rangos de presupuesto estimado para no pedir una cifra exacta al cliente. */
public enum BudgetRange {
    UNDER_50M,
    FROM_50M_TO_100M,
    FROM_100M_TO_250M,
    FROM_250M_TO_500M,
    OVER_500M
}
