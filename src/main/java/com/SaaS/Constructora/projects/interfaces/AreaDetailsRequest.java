package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.domain.CapituloPresupuestal;
import com.SaaS.Constructora.projects.domain.CategoriaInsumo;
import com.SaaS.Constructora.projects.domain.Disciplina;
import com.SaaS.Constructora.projects.domain.PermisoSst;
import com.SaaS.Constructora.projects.domain.TipoDocumentoContable;
import com.SaaS.Constructora.projects.domain.UnidadMedida;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Campos específicos que se pueden cargar al crear una tarea. Solo se aceptan
 * los campos del tipo de área seleccionado; la validación contextual se hace en
 * {@code TaskAreaDetailsValidator}.
 */
public record AreaDetailsRequest(
        @Size(max = 300, message = "El frente de obra no puede superar 300 caracteres")
        String frenteObra,

        CapituloPresupuestal capituloPresupuestal,
        UnidadMedida unidadMedida,

        @PositiveOrZero(message = "La cantidad programada no puede ser negativa")
        BigDecimal cantidadProgramada,

        UUID contratistaId,
        Boolean requierePermisoSst,

        @Size(max = 20, message = "Máximo 20 permisos SST por tarea")
        Set<PermisoSst> permisosSst,

        @Size(max = 80, message = "El código de requisición no puede superar 80 caracteres")
        String codigoRequisicion,

        LocalDate fechaRequeridaEnObra,
        CategoriaInsumo categoriaInsumo,

        @PositiveOrZero(message = "El presupuesto estimado no puede ser negativo")
        BigDecimal presupuestoEstimado,

        @Size(max = 150, message = "El proveedor sugerido no puede superar 150 caracteres")
        String proveedorSugerido,

        Disciplina disciplina,

        @Size(max = 80, message = "El código de plano no puede superar 80 caracteres")
        String codigoPlano,

        @Size(max = 20, message = "La versión del plano no puede superar 20 caracteres")
        String versionPlano,

        @Size(max = 150, message = "La entidad no puede superar 150 caracteres")
        String entidad,

        @Size(max = 80, message = "El número de radicado no puede superar 80 caracteres")
        String numeroRadicado,

        TipoDocumentoContable tipoDocumento,

        @Size(max = 80, message = "El número de documento no puede superar 80 caracteres")
        String numeroDocumento,

        @PositiveOrZero(message = "El monto no puede ser negativo")
        BigDecimal monto
) { }
