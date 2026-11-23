package com.SaaS.Constructora.projects.infrastructure;

import com.SaaS.Constructora.projects.domain.CapituloPresupuestal;
import com.SaaS.Constructora.projects.domain.CategoriaInsumo;
import com.SaaS.Constructora.projects.domain.Disciplina;
import com.SaaS.Constructora.projects.domain.PermisoSst;
import com.SaaS.Constructora.projects.domain.TipoDocumentoContable;
import com.SaaS.Constructora.projects.domain.UnidadMedida;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

/**
 * Campos específicos que se cargan en una tarea según el tipo de área elegida.
 * Se mapea como value object embebido: todas las columnas viven en {@code tasks}
 * y las que no corresponden al área quedan en null.
 */
@Embeddable
public class TaskAreaDetails {

    @Column(name = "frente_obra", length = 300)
    private String frenteObra;

    @Enumerated(EnumType.STRING)
    @Column(name = "capitulo_presupuestal", length = 40)
    private CapituloPresupuestal capituloPresupuestal;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", length = 20)
    private UnidadMedida unidadMedida;

    @Column(name = "cantidad_programada", precision = 18, scale = 3)
    private BigDecimal cantidadProgramada;

    /** Contratista o cuadrilla; se guarda el identificador sin FK entre módulos. */
    @Column(name = "contratista_id")
    private UUID contratistaId;

    @Column(name = "requiere_permiso_sst", nullable = false)
    private boolean requierePermisoSst;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "permisos_sst", columnDefinition = "varchar(40)[]")
    private String[] permisosSst;

    @Column(name = "codigo_requisicion", length = 80)
    private String codigoRequisicion;

    @Column(name = "fecha_requerida_en_obra")
    private LocalDate fechaRequeridaEnObra;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_insumo", length = 40)
    private CategoriaInsumo categoriaInsumo;

    @Column(name = "presupuesto_estimado", precision = 18, scale = 2)
    private BigDecimal presupuestoEstimado;

    @Column(name = "proveedor_sugerido", length = 150)
    private String proveedorSugerido;

    @Enumerated(EnumType.STRING)
    @Column(name = "disciplina", length = 40)
    private Disciplina disciplina;

    @Column(name = "codigo_plano", length = 80)
    private String codigoPlano;

    @Column(name = "version_plano", length = 20)
    private String versionPlano;

    @Column(name = "entidad", length = 150)
    private String entidad;

    @Column(name = "numero_radicado", length = 80)
    private String numeroRadicado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 40)
    private TipoDocumentoContable tipoDocumento;

    @Column(name = "numero_documento", length = 80)
    private String numeroDocumento;

    @Column(name = "monto", precision = 18, scale = 2)
    private BigDecimal monto;

    public TaskAreaDetails() { } // requerido por JPA

    public TaskAreaDetails(String frenteObra, CapituloPresupuestal capituloPresupuestal,
                           UnidadMedida unidadMedida, BigDecimal cantidadProgramada,
                           UUID contratistaId, boolean requierePermisoSst,
                           java.util.Set<PermisoSst> permisosSst,
                           String codigoRequisicion, LocalDate fechaRequeridaEnObra,
                           CategoriaInsumo categoriaInsumo, BigDecimal presupuestoEstimado,
                           String proveedorSugerido, Disciplina disciplina, String codigoPlano,
                           String versionPlano, String entidad, String numeroRadicado,
                           TipoDocumentoContable tipoDocumento, String numeroDocumento,
                           BigDecimal monto) {
        this.frenteObra = frenteObra;
        this.capituloPresupuestal = capituloPresupuestal;
        this.unidadMedida = unidadMedida;
        this.cantidadProgramada = cantidadProgramada;
        this.contratistaId = contratistaId;
        this.requierePermisoSst = requierePermisoSst;
        this.permisosSst = permisosSst == null || permisosSst.isEmpty()
                ? new String[0]
                : permisosSst.stream().map(Enum::name).toArray(String[]::new);
        this.codigoRequisicion = codigoRequisicion;
        this.fechaRequeridaEnObra = fechaRequeridaEnObra;
        this.categoriaInsumo = categoriaInsumo;
        this.presupuestoEstimado = presupuestoEstimado;
        this.proveedorSugerido = proveedorSugerido;
        this.disciplina = disciplina;
        this.codigoPlano = codigoPlano;
        this.versionPlano = versionPlano;
        this.entidad = entidad;
        this.numeroRadicado = numeroRadicado;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.monto = monto;
    }

    public String getFrenteObra() { return frenteObra; }
    public CapituloPresupuestal getCapituloPresupuestal() { return capituloPresupuestal; }
    public UnidadMedida getUnidadMedida() { return unidadMedida; }
    public BigDecimal getCantidadProgramada() { return cantidadProgramada; }
    public UUID getContratistaId() { return contratistaId; }
    public boolean isRequierePermisoSst() { return requierePermisoSst; }

    public java.util.List<PermisoSst> getPermisosSst() {
        return permisosSst == null ? java.util.List.of()
                : Arrays.stream(permisosSst).map(PermisoSst::valueOf).toList();
    }

    public String getCodigoRequisicion() { return codigoRequisicion; }
    public LocalDate getFechaRequeridaEnObra() { return fechaRequeridaEnObra; }
    public CategoriaInsumo getCategoriaInsumo() { return categoriaInsumo; }
    public BigDecimal getPresupuestoEstimado() { return presupuestoEstimado; }
    public String getProveedorSugerido() { return proveedorSugerido; }
    public Disciplina getDisciplina() { return disciplina; }
    public String getCodigoPlano() { return codigoPlano; }
    public String getVersionPlano() { return versionPlano; }
    public String getEntidad() { return entidad; }
    public String getNumeroRadicado() { return numeroRadicado; }
    public TipoDocumentoContable getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public BigDecimal getMonto() { return monto; }
}
