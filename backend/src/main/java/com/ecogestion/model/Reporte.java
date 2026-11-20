package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.ReglaNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reporte de acumulación de basura o residuos abandonados ({@code «Document» Reporte} de las vistas
 * lógica y de desarrollo). Contiene las reglas de negocio del ciclo de vida del reporte.
 */
@Document("reportes")
public class Reporte {

    public static final int MAX_FOTOS = 5;

    @Id
    private String id;
    private String descripcion;
    private TipoResiduo tipoResiduo;
    private Prioridad prioridad;
    private double latitud;
    private double longitud;
    private String direccion;
    private String sector;
    /** Punto GeoJSON con índice 2dsphere para buscar duplicados cercanos (HU-06). */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;
    private List<String> fotos = new ArrayList<>();
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaResolucion;
    private EstadoReporte estado = EstadoReporte.PENDIENTE;
    private String descripcionTrabajo;
    private List<String> fotosEvidencia = new ArrayList<>();
    @DBRef
    private Ciudadano ciudadano;
    @DBRef
    private Operador operador;
    @DBRef
    private Reporte reporteOriginal;
    /** Control de concurrencia optimista: impide que dos asignaciones simultáneas se guarden (HU-07). */
    @Version
    private Long version;

    protected Reporte() {
    }

    public Reporte(LocalDateTime fechaReporte, Ciudadano ciudadano, String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new DatosInvalidosException("La descripción es obligatoria");
        }
        this.fechaReporte = Objects.requireNonNull(fechaReporte);
        this.ciudadano = Objects.requireNonNull(ciudadano);
        this.descripcion = descripcion.trim();
    }

    // ------------------------------------------------------------------ ubicación y fotos (HU-03, HU-04)

    public void registrarUbicacion(double latitud, double longitud, String direccion, String sector) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
        this.sector = sector;
        this.ubicacion = new GeoJsonPoint(longitud, latitud);
    }

    public void adjuntarFotos(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new DatosInvalidosException("Debe adjuntar al menos 1 foto");
        }
        if (urls.size() > MAX_FOTOS) {
            throw new DatosInvalidosException("Puede adjuntar como máximo " + MAX_FOTOS + " fotos");
        }
        this.fotos = new ArrayList<>(urls);
    }

    // ------------------------------------------------------------------ clasificación (HU-05)

    public void clasificar(TipoResiduo tipo, Prioridad nuevaPrioridad) {
        if (tipo == null || nuevaPrioridad == null) {
            throw new DatosInvalidosException("El tipo de residuo y la prioridad son obligatorios");
        }
        if (!estado.estaAbierto()) {
            throw new ReglaNegocioException("No se puede reclasificar un reporte resuelto");
        }
        if (tipo.esPeligroso() && nuevaPrioridad != Prioridad.ALTA) {
            throw new ReglaNegocioException("Los residuos peligrosos siempre tienen prioridad Alta");
        }
        this.tipoResiduo = tipo;
        this.prioridad = nuevaPrioridad;
    }

    // ------------------------------------------------------------------ estados (HU-09)

    public void cambiarEstado(EstadoReporte nuevo) {
        if (!estado.puedeAvanzarA(nuevo)) {
            throw new ReglaNegocioException("No se puede pasar de " + estado + " a " + nuevo
                    + ": el flujo es Pendiente → Asignado → En proceso → Resuelto");
        }
        this.estado = nuevo;
    }

    public boolean estaPendiente() {
        return estado == EstadoReporte.PENDIENTE && !esDuplicado();
    }

    // ------------------------------------------------------------------ asignación (HU-07)

    /** Asigna el reporte a un operador; un reporte nunca queda con dos operadores. */
    public void asignar(Operador nuevoOperador) {
        Objects.requireNonNull(nuevoOperador);
        if (esDuplicado()) {
            throw new ReglaNegocioException("Un reporte duplicado no puede asignarse a un operador");
        }
        if (operador != null) {
            throw new ReglaNegocioException("El reporte ya está asignado a " + operador.getNombre());
        }
        cambiarEstado(EstadoReporte.ASIGNADO);
        this.operador = nuevoOperador;
    }

    /** Cambia de operador un reporte que todavía no está resuelto. */
    public void reasignar(Operador nuevoOperador) {
        Objects.requireNonNull(nuevoOperador);
        if (operador == null) {
            asignar(nuevoOperador);
            return;
        }
        if (estado == EstadoReporte.RESUELTO) {
            throw new ReglaNegocioException("No se puede reasignar un reporte resuelto");
        }
        this.operador = nuevoOperador;
    }

    // ------------------------------------------------------------------ atención (HU-09)

    public void iniciarAtencion() {
        cambiarEstado(EstadoReporte.EN_PROCESO);
    }

    public void registrarCierre(String descripcionDelTrabajo, List<String> evidencias) {
        if (descripcionDelTrabajo == null || descripcionDelTrabajo.isBlank()) {
            throw new DatosInvalidosException("La descripción del trabajo realizado es obligatoria");
        }
        if (evidencias == null || evidencias.isEmpty() || evidencias.size() > MAX_FOTOS) {
            throw new DatosInvalidosException("Debe adjuntar de 1 a " + MAX_FOTOS + " fotos de evidencia");
        }
        cambiarEstado(EstadoReporte.RESUELTO);
        this.descripcionTrabajo = descripcionDelTrabajo.trim();
        this.fotosEvidencia = new ArrayList<>(evidencias);
        this.fechaResolucion = LocalDateTime.now();
    }

    // ------------------------------------------------------------------ duplicados (HU-06)

    public void marcarComoDuplicado(Reporte original) {
        Objects.requireNonNull(original);
        if (original == this || (id != null && id.equals(original.getId()))) {
            throw new ReglaNegocioException("Un reporte no puede ser duplicado de sí mismo");
        }
        if (original.esDuplicado()) {
            throw new ReglaNegocioException("El reporte original no puede ser a su vez un duplicado");
        }
        if (estado != EstadoReporte.PENDIENTE || operador != null) {
            throw new ReglaNegocioException("Solo un reporte pendiente y sin operador puede marcarse como duplicado");
        }
        this.reporteOriginal = original;
    }

    public void desvincularDuplicado() {
        if (!esDuplicado()) {
            throw new ReglaNegocioException("El reporte no está marcado como duplicado");
        }
        this.reporteOriginal = null;
        this.estado = EstadoReporte.PENDIENTE;
    }

    public boolean esDuplicado() {
        return reporteOriginal != null;
    }

    /** Un duplicado refleja el estado de su reporte original (HU-06, criterio 3). */
    public EstadoReporte estadoVisible() {
        return esDuplicado() ? reporteOriginal.getEstado() : estado;
    }

    // ------------------------------------------------------------------ consultas

    public boolean esDelCiudadano(String ciudadanoId) {
        return ciudadano != null && ciudadano.getId() != null && ciudadano.getId().equals(ciudadanoId);
    }

    public boolean estaAsignadoA(String operadorId) {
        return operador != null && operador.getId() != null && operador.getId().equals(operadorId);
    }

    /** Horas entre la creación y la resolución (HU-15: tiempo de atención). */
    public Double horasDeAtencion() {
        if (fechaResolucion == null) {
            return null;
        }
        return Duration.between(fechaReporte, fechaResolucion).toMinutes() / 60.0;
    }

    // ------------------------------------------------------------------ getters / setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoResiduo getTipoResiduo() { return tipoResiduo; }
    public void setTipoResiduo(TipoResiduo tipoResiduo) { this.tipoResiduo = tipoResiduo; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public String getDireccion() { return direccion; }
    public String getSector() { return sector; }
    public GeoJsonPoint getUbicacion() { return ubicacion; }
    public List<String> getFotos() { return fotos; }
    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }
    public String getDescripcionTrabajo() { return descripcionTrabajo; }
    public List<String> getFotosEvidencia() { return fotosEvidencia; }
    public Ciudadano getCiudadano() { return ciudadano; }
    public void setCiudadano(Ciudadano ciudadano) { this.ciudadano = ciudadano; }
    public Operador getOperador() { return operador; }
    public void setOperador(Operador operador) { this.operador = operador; }
    public Reporte getReporteOriginal() { return reporteOriginal; }
    public Long getVersion() { return version; }
}
