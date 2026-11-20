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
 * Reporte de basura acumulada o residuos abandonados que crea un ciudadano.
 * Es la clase central del sistema ({@code «Document» Reporte} en las vistas lógica y de desarrollo)
 * y se guarda en la colección {@code reportes} de MongoDB.
 *
 * <p><b>Cómo funciona:</b> el reporte nace en estado PENDIENTE y solo puede avanzar en orden:
 * PENDIENTE → ASIGNADO → EN_PROCESO → RESUELTO. Cada método público de negocio (asignar,
 * iniciarAtencion, registrarCierre, marcarComoDuplicado...) valida sus reglas y lanza una excepción
 * si la operación no está permitida, así el reporte nunca queda en un estado inválido.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Experto en información:</b> el reporte conoce su estado, su operador, sus fotos y su
 *       reporte original, por eso es él quien decide si se puede asignar, cerrar o marcar como
 *       duplicado. Los servicios no repiten estas reglas: solo le piden al reporte que actúe.</li>
 *   <li><b>Alta cohesión:</b> todo lo que cambia el ciclo de vida del reporte está en esta clase;
 *       guardar en la base de datos o enviar correos queda fuera (servicios y repositorios).</li>
 * </ul>
 */
@Document("reportes")
public class Reporte {

    /** Máximo de fotos por reporte y de fotos de evidencia (HU-03 y HU-09). */
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
    /** La misma ubicación en formato GeoJSON; el índice 2dsphere permite buscar reportes cercanos (HU-06). */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;
    private List<String> fotos = new ArrayList<>();
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaResolucion;
    private EstadoReporte estado = EstadoReporte.PENDIENTE;
    private String descripcionTrabajo;
    private List<String> fotosEvidencia = new ArrayList<>();
    /** @DBRef guarda solo la referencia al documento del ciudadano (asociación "historial"). */
    @DBRef
    private Ciudadano ciudadano;
    /** Operador que atiende el reporte (0..1): nunca hay dos operadores para el mismo reporte (HU-07). */
    @DBRef
    private Operador operador;
    /** Si no es null, este reporte es un duplicado de otro (asociación "duplicadoDe", HU-06). */
    @DBRef
    private Reporte reporteOriginal;
    /**
     * Control de concurrencia optimista: MongoDB rechaza el guardado si otro usuario modificó el
     * reporte al mismo tiempo. Así dos administradores no pueden asignarlo a la vez (HU-07).
     */
    @Version
    private Long version;

    /** Constructor vacío que necesita Spring Data para leer el documento desde MongoDB. */
    protected Reporte() {
    }

    /** Crea un reporte nuevo en estado PENDIENTE. La descripción es obligatoria (HU-03). */
    public Reporte(LocalDateTime fechaReporte, Ciudadano ciudadano, String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new DatosInvalidosException("La descripción es obligatoria");
        }
        this.fechaReporte = Objects.requireNonNull(fechaReporte);
        this.ciudadano = Objects.requireNonNull(ciudadano);
        this.descripcion = descripcion.trim();
    }

    // ===================================================== Ubicación y fotos (HU-03, HU-04)

    /** Guarda la ubicación del GPS o del mapa junto con la dirección y el sector calculados. */
    public void registrarUbicacion(double latitud, double longitud, String direccion, String sector) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
        this.sector = sector;
        this.ubicacion = new GeoJsonPoint(longitud, latitud); // GeoJSON usa el orden (longitud, latitud)
    }

    /** Adjunta las URL de las fotos ya guardadas: entre 1 y 5. */
    public void adjuntarFotos(List<String> urls) {
        validarCantidadDeFotos(urls, "Debe adjuntar de 1 a " + MAX_FOTOS + " fotos");
        this.fotos = new ArrayList<>(urls);
    }

    // ===================================================== Clasificación (HU-05)

    /** Asigna el tipo de residuo y la prioridad. Un residuo peligroso siempre es de prioridad ALTA. */
    public void clasificar(TipoResiduo tipo, Prioridad nuevaPrioridad) {
        if (tipo == null || nuevaPrioridad == null) {
            throw new DatosInvalidosException("El tipo de residuo y la prioridad son obligatorios");
        }
        if (estado == EstadoReporte.RESUELTO) {
            throw new ReglaNegocioException("No se puede reclasificar un reporte resuelto");
        }
        if (tipo.esPeligroso() && nuevaPrioridad != Prioridad.ALTA) {
            throw new ReglaNegocioException("Los residuos peligrosos siempre tienen prioridad Alta");
        }
        this.tipoResiduo = tipo;
        this.prioridad = nuevaPrioridad;
    }

    // ===================================================== Estados (HU-09)

    /** Avanza al estado indicado. El enum EstadoReporte decide si el salto está permitido. */
    public void cambiarEstado(EstadoReporte nuevo) {
        if (!estado.puedeAvanzarA(nuevo)) {
            throw new ReglaNegocioException("No se puede pasar de " + estado + " a " + nuevo
                    + ": el flujo es Pendiente → Asignado → En proceso → Resuelto");
        }
        this.estado = nuevo;
    }

    /** Un reporte está pendiente de revisión si no se ha asignado y no es un duplicado. */
    public boolean estaPendiente() {
        return estado == EstadoReporte.PENDIENTE && !esDuplicado();
    }

    // ===================================================== Asignación (HU-07)

    /** Asigna el reporte a un operador y lo pasa a ASIGNADO. Falla si ya tiene operador. */
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

    /** Cambia el operador de un reporte que aún no está resuelto (si no tenía, lo asigna). */
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

    // ===================================================== Atención del operador (HU-09)

    /** El operador empieza a trabajar: ASIGNADO → EN_PROCESO. */
    public void iniciarAtencion() {
        cambiarEstado(EstadoReporte.EN_PROCESO);
    }

    /** El operador termina: guarda lo que hizo y sus fotos de evidencia, y pasa a RESUELTO. */
    public void registrarCierre(String descripcionDelTrabajo, List<String> evidencias) {
        if (descripcionDelTrabajo == null || descripcionDelTrabajo.isBlank()) {
            throw new DatosInvalidosException("La descripción del trabajo realizado es obligatoria");
        }
        validarCantidadDeFotos(evidencias, "Debe adjuntar de 1 a " + MAX_FOTOS + " fotos de evidencia");
        cambiarEstado(EstadoReporte.RESUELTO);
        this.descripcionTrabajo = descripcionDelTrabajo.trim();
        this.fotosEvidencia = new ArrayList<>(evidencias);
        this.fechaResolucion = LocalDateTime.now();
    }

    // ===================================================== Duplicados (HU-06)

    /** Vincula este reporte a otro que describe el mismo problema. */
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

    /** Deshace la vinculación: el reporte vuelve a ser independiente y PENDIENTE. */
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

    /** Estado que ve el ciudadano: un duplicado muestra el estado de su reporte original (HU-06). */
    public EstadoReporte estadoVisible() {
        return esDuplicado() ? reporteOriginal.getEstado() : estado;
    }

    // ===================================================== Consultas de apoyo

    public boolean esDelCiudadano(String ciudadanoId) {
        return ciudadano != null && Objects.equals(ciudadano.getId(), ciudadanoId);
    }

    public boolean estaAsignadoA(String operadorId) {
        return operador != null && Objects.equals(operador.getId(), operadorId);
    }

    /** Horas que pasaron entre la creación y la resolución (indicador de HU-15). */
    public Double horasDeAtencion() {
        if (fechaResolucion == null) {
            return null;
        }
        return Duration.between(fechaReporte, fechaResolucion).toMinutes() / 60.0;
    }

    private static void validarCantidadDeFotos(List<String> urls, String mensaje) {
        if (urls == null || urls.isEmpty() || urls.size() > MAX_FOTOS) {
            throw new DatosInvalidosException(mensaje);
        }
    }

    // ===================================================== Getters y setters
    // Solo existen los setters que usan las pruebas o la persistencia; los cambios de negocio
    // se hacen con los métodos de arriba para que siempre se validen las reglas.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public TipoResiduo getTipoResiduo() { return tipoResiduo; }
    public Prioridad getPrioridad() { return prioridad; }
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
    public String getDescripcionTrabajo() { return descripcionTrabajo; }
    public List<String> getFotosEvidencia() { return fotosEvidencia; }
    public Ciudadano getCiudadano() { return ciudadano; }
    public Operador getOperador() { return operador; }
    public Reporte getReporteOriginal() { return reporteOriginal; }
    public Long getVersion() { return version; }
}
