package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sector de la ciudad que agrupa varios barrios (colección {@code sectores}, HU-12).
 * Su límite es un polígono GeoJSON con índice 2dsphere: MongoDB puede responder "¿en qué sector cae
 * esta coordenada?", que es el servicio "Identificar sector" de la vista funcional (HU-04).
 *
 * <p><b>Patrón GRASP — Experto en información:</b> el sector conoce sus barrios, por eso es él quien
 * responde si incluye un barrio ({@link #incluyeBarrio}).
 */
@Document("sectores")
public class Sector {

    @Id
    private String id;
    @Indexed(unique = true)
    private String nombre;
    private String comuna;
    private List<String> barrios = new ArrayList<>();
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPolygon limite;

    protected Sector() {
    }

    public Sector(String nombre, String comuna, List<String> barrios, GeoJsonPolygon limite) {
        this.nombre = nombre;
        this.comuna = comuna;
        this.barrios = new ArrayList<>(barrios);
        this.limite = limite;
    }

    public boolean incluyeBarrio(String barrio) {
        return barrios.stream().anyMatch(b -> b.equalsIgnoreCase(barrio));
    }

    // Consultas de solo lectura: no hay setters (los cambios pasan por los métodos de arriba, que validan).
    // Los getters existen para armar las respuestas de la API; las colecciones se devuelven inmodificables.
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getComuna() { return comuna; }
    public List<String> getBarrios() { return Collections.unmodifiableList(barrios); }
}
