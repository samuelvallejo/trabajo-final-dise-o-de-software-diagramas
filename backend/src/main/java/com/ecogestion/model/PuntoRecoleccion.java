package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.EnumSet;
import java.util.Set;

/**
 * Punto de recolección autorizado: ubicación, materiales que recibe y horario de atención
 * (colección {@code puntos_recoleccion}, HU-13). Un punto desactivado deja de mostrarse a los ciudadanos.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> el punto conoce sus materiales, así que es él quien
 * responde si recibe un material ({@link #recibeMaterial}) y quien valida sus propios datos.
 */
@Document("puntos_recoleccion")
public class PuntoRecoleccion {

    @Id
    private String id;
    private String nombre;
    private String direccion;
    private String sector;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion;
    private Set<MaterialAceptado> materiales = EnumSet.noneOf(MaterialAceptado.class);
    private String horarioAtencion;
    private boolean activo = true;

    protected PuntoRecoleccion() {
    }

    public PuntoRecoleccion(String nombre, String direccion, String sector, double latitud, double longitud,
                            Set<MaterialAceptado> materiales, String horarioAtencion) {
        actualizar(nombre, direccion, sector, latitud, longitud, materiales, horarioAtencion);
    }

    public void actualizar(String nuevoNombre, String nuevaDireccion, String nuevoSector, double latitud,
                           double longitud, Set<MaterialAceptado> nuevosMateriales, String nuevoHorario) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del punto es obligatorio");
        }
        if (nuevosMateriales == null || nuevosMateriales.isEmpty()) {
            throw new DatosInvalidosException("El punto debe recibir al menos un material");
        }
        this.nombre = nuevoNombre.trim();
        this.direccion = nuevaDireccion;
        this.sector = nuevoSector;
        this.ubicacion = new GeoJsonPoint(longitud, latitud);
        this.materiales = EnumSet.copyOf(nuevosMateriales);
        this.horarioAtencion = nuevoHorario;
    }

    public boolean recibeMaterial(MaterialAceptado material) {
        return materiales.contains(material);
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getSector() { return sector; }
    public GeoJsonPoint getUbicacion() { return ubicacion; }
    public double getLatitud() { return ubicacion.getY(); }
    public double getLongitud() { return ubicacion.getX(); }
    public Set<MaterialAceptado> getMateriales() { return materiales; }
    public String getHorarioAtencion() { return horarioAtencion; }
    public boolean isActivo() { return activo; }
}
