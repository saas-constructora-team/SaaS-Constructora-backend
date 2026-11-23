package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.projects.application.TaskAreaDetailsValidator;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.domain.CapituloPresupuestal;
import com.SaaS.Constructora.projects.domain.Disciplina;
import com.SaaS.Constructora.projects.domain.PermisoSst;
import com.SaaS.Constructora.projects.domain.TipoDocumentoContable;
import com.SaaS.Constructora.projects.interfaces.AreaDetailsRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskAreaDetailsValidatorTest {

    private static AreaDetailsRequest empty() {
        return new AreaDetailsRequest(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void aceptaCamposDelAreaObra() {
        AreaDetailsRequest details = new AreaDetailsRequest("Torre 1", CapituloPresupuestal.ESTRUCTURA,
                null, new BigDecimal("10"), null, true, Set.of(PermisoSst.ESPACIO_CONFINADO),
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(details, AreaTipo.OBRA_INGENIERIA));
    }

    @Test
    void aceptaCamposDeCadaAreaPredeterminada() {
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(
                new AreaDetailsRequest(null, null, null, null, null, null, null, "REQ-1", null,
                        null, null, null, null, null, null, null, null, null, null, null),
                AreaTipo.COMPRAS_ALMACEN));
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(
                new AreaDetailsRequest(null, null, null, null, null, null, null, null, null,
                        null, null, null, Disciplina.ARQUITECTONICO, "ARQ-102", "Rev. 0",
                        null, null, null, null, null),
                AreaTipo.DISENO_ARQUITECTURA));
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(
                new AreaDetailsRequest(null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "Curaduría", null, null, null, null),
                AreaTipo.LEGAL_TRAMITES));
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(
                new AreaDetailsRequest(null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        TipoDocumentoContable.ACTA_CORTE_OBRA, "AC-1", new BigDecimal("1000")),
                AreaTipo.CONTABILIDAD_FINANZAS));
    }

    @Test
    void aceptaDetallesVaciosEnAreaPersonalizadaYSinDetalles() {
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(empty(), AreaTipo.PERSONALIZADA));
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(null, AreaTipo.PERSONALIZADA));
        assertDoesNotThrow(() -> TaskAreaDetailsValidator.validate(null, null));
    }

    @Test
    void rechazaCamposAjenosAlArea() {
        AreaDetailsRequest details = new AreaDetailsRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, "Curaduría",
                null, null, null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> TaskAreaDetailsValidator.validate(details, AreaTipo.COMPRAS_ALMACEN));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaCamposEnAreaPersonalizada() {
        AreaDetailsRequest details = new AreaDetailsRequest("Torre 1", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> TaskAreaDetailsValidator.validate(details, AreaTipo.PERSONALIZADA));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaCamposCuandoNoHayArea() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> TaskAreaDetailsValidator.validate(
                        new AreaDetailsRequest(null, null, null, null, null, null, null,
                                null, null, null, null, null, null, null, null, "Notaría",
                                null, null, null, null), null));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaPermisosSstSinMarcarRequerimiento() {
        AreaDetailsRequest details = new AreaDetailsRequest(null, null, null, null, null, false,
                Set.of(PermisoSst.TRABAJO_EN_CALIENTE), null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> TaskAreaDetailsValidator.validate(details, AreaTipo.OBRA_INGENIERIA));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
