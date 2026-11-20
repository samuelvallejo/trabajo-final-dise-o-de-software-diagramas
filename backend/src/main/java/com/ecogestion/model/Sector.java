package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Sector de la ciudad que agrupa varios barrios (HU-12). Su límite permite identificar el sector
 * de un reporte a partir de su ubicación ("Identificar sector" en la vista funcional).
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

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getComuna() { return comuna; }
    public List<String> getBarrios() { return barrios; }
    public GeoJsonPolygon getLimite() { return limite; }
}
