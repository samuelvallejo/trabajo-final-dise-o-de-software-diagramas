package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Ciudadano que reporta problemas de residuos (vistas lógica y de desarrollo: {@code «Document» Ciudadano}).
 * Atributos: nombre, documento, correo, contrasenaHash (HU-01) y sector de residencia (HU-02).
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
