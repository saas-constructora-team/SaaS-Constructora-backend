package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.domain.CapituloPresupuestal;
import com.SaaS.Constructora.projects.domain.CategoriaInsumo;
import com.SaaS.Constructora.projects.domain.Disciplina;
import com.SaaS.Constructora.projects.domain.PermisoSst;
import com.SaaS.Constructora.projects.domain.TipoDocumentoContable;
import com.SaaS.Constructora.projects.domain.UnidadMedida;
import com.SaaS.Constructora.projects.infrastructure.TaskAreaDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Detalles del área de la tarea. Solo se rellenan los campos de su tipo de área. */
public record AreaDetailsResponse(
        String frenteObra,
        CapituloPresupuestal capituloPresupuestal,
        UnidadMedida unidadMedida,
        BigDecimal cantidadProgramada,
        UUID contratistaId,
        boolean requierePermisoSst,
        List<PermisoSst> permisosSst,
        String codigoRequisicion,
        LocalDate fechaRequeridaEnObra,
        CategoriaInsumo categoriaInsumo,
        BigDecimal presupuestoEstimado,
        String proveedorSugerido,
        Disciplina disciplina,
        String codigoPlano,
        String versionPlano,
        String entidad,
        String numeroRadicado,
        TipoDocumentoContable tipoDocumento,
        String numeroDocumento,
        BigDecimal monto
) {
    public static AreaDetailsResponse from(TaskAreaDetails details) {
        return new AreaDetailsResponse(
                details.getFrenteObra(),
                details.getCapituloPresupuestal(),
                details.getUnidadMedida(),
                details.getCantidadProgramada(),
                details.getContratistaId(),
                details.isRequierePermisoSst(),
                details.getPermisosSst(),
                details.getCodigoRequisicion(),
                details.getFechaRequeridaEnObra(),
                details.getCategoriaInsumo(),
                details.getPresupuestoEstimado(),
                details.getProveedorSugerido(),
                details.getDisciplina(),
                details.getCodigoPlano(),
                details.getVersionPlano(),
                details.getEntidad(),
                details.getNumeroRadicado(),
                details.getTipoDocumento(),
                details.getNumeroDocumento(),
                details.getMonto());
    }
}
