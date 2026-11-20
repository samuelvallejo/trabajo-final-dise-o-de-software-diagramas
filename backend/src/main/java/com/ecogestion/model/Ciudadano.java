package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Ciudadano que reporta problemas de residuos (colección {@code ciudadanos}).
 * Además de los datos de {@link Usuario}, guarda el sector donde vive para mostrarle sus horarios
 * y puntos de recolección (HU-02, HU-12, HU-13).
 *
 * <p><b>Patrón GRASP — Polimorfismo:</b> sobrescribe {@link #getRol()} para identificarse como CIUDADANO.
 */
@Document("ciudadanos")
public class Ciudadano extends Usuario {

    private String sector;

    protected Ciudadano() {
    }

    public Ciudadano(String nombre, String documento, String correo, String contrasenaHash) {
        super(nombre, documento, correo, contrasenaHash);
    }

    @Override
    public Rol getRol() {
        return Rol.CIUDADANO;
    }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
}
