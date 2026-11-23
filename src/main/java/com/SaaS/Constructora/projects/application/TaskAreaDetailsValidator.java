package com.SaaS.Constructora.projects.application;

import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.interfaces.AreaDetailsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida los campos específicos de una tarea contra el tipo del área elegida.
 * No exige que el usuario llene todos los campos: solo rechaza los campos que
 * pertenecen a otra área y las combinaciones incoherentes.
 */
public final class TaskAreaDetailsValidator {

    private static final Map<AreaTipo, Set<String>> CAMPOS_POR_AREA = Map.of(
            AreaTipo.OBRA_INGENIERIA, Set.of("frenteObra", "capituloPresupuestal", "unidadMedida",
                    "cantidadProgramada", "contratistaId", "requierePermisoSst", "permisosSst"),
            AreaTipo.COMPRAS_ALMACEN, Set.of("codigoRequisicion", "fechaRequeridaEnObra",
                    "categoriaInsumo", "presupuestoEstimado", "proveedorSugerido"),
            AreaTipo.DISENO_ARQUITECTURA, Set.of("disciplina", "codigoPlano", "versionPlano"),
            AreaTipo.LEGAL_TRAMITES, Set.of("entidad", "numeroRadicado"),
            AreaTipo.CONTABILIDAD_FINANZAS, Set.of("tipoDocumento", "numeroDocumento", "monto"),
            AreaTipo.PERSONALIZADA, Set.of());

    private TaskAreaDetailsValidator() { }

    public static void validate(AreaDetailsRequest details, AreaTipo tipo) {
        if (details == null) {
            return;
        }
        Map<String, Boolean> enviados = camposEnviados(details);
        List<String> presentes = enviados.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
        if (presentes.isEmpty()) {
            return;
        }
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se debe seleccionar un área para cargar campos específicos");
        }

        Set<String> permitidos = CAMPOS_POR_AREA.getOrDefault(tipo, Set.of());
        List<String> ajenos = new ArrayList<>(presentes.stream()
                .filter(campo -> !permitidos.contains(campo))
                .toList());
        if (!ajenos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los campos " + String.join(", ", ajenos)
                            + " no corresponden al área " + tipo);
        }
        if (details.permisosSst() != null && !details.permisosSst().isEmpty()
                && !Boolean.TRUE.equals(details.requierePermisoSst())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para indicar permisos SST debe marcar requierePermisoSst");
        }
    }

    private static Map<String, Boolean> camposEnviados(AreaDetailsRequest d) {
        Map<String, Boolean> enviados = new LinkedHashMap<>();
        enviados.put("frenteObra", d.frenteObra() != null);
        enviados.put("capituloPresupuestal", d.capituloPresupuestal() != null);
        enviados.put("unidadMedida", d.unidadMedida() != null);
        enviados.put("cantidadProgramada", d.cantidadProgramada() != null);
        enviados.put("contratistaId", d.contratistaId() != null);
        enviados.put("requierePermisoSst", d.requierePermisoSst() != null);
        enviados.put("permisosSst", d.permisosSst() != null && !d.permisosSst().isEmpty());
        enviados.put("codigoRequisicion", d.codigoRequisicion() != null);
        enviados.put("fechaRequeridaEnObra", d.fechaRequeridaEnObra() != null);
        enviados.put("categoriaInsumo", d.categoriaInsumo() != null);
        enviados.put("presupuestoEstimado", d.presupuestoEstimado() != null);
        enviados.put("proveedorSugerido", d.proveedorSugerido() != null);
        enviados.put("disciplina", d.disciplina() != null);
        enviados.put("codigoPlano", d.codigoPlano() != null);
        enviados.put("versionPlano", d.versionPlano() != null);
        enviados.put("entidad", d.entidad() != null);
        enviados.put("numeroRadicado", d.numeroRadicado() != null);
        enviados.put("tipoDocumento", d.tipoDocumento() != null);
        enviados.put("numeroDocumento", d.numeroDocumento() != null);
        enviados.put("monto", d.monto() != null);
        return enviados;
    }
}
