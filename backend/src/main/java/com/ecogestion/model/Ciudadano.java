package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Ciudadano que reporta problemas de residuos (colección {@code ciudadanos}).
 * Además de los datos de {@link Usuario}, guarda el sector donde vive para mostrarle sus horarios
 * y puntos de recolección (HU-02, HU-12, HU-13).
 *
 * <p><b>Encapsulamiento:</b> en vez de {@code setNombre}, {@code setCorreo} y {@code setSector} sueltos,
 * hay un solo método {@link #actualizarPerfil} que representa la acción real del caso de uso "Mi perfil"
 * y valida los datos juntos.
 *
 * <p><b>Patrón GRASP — Polimorfismo:</b> sobrescribe {@link #getRol()} para identificarse como CIUDADANO.
 */
@Document("ciudadanos")
public class Ciudadano extends Usuario {

    private String sector;

    protected Ciudadano() {
    }

    /** El sector es opcional (puede ser null); si viene, el servicio ya verificó que exista. */
    public Ciudadano(String nombre, String documento, String correo, String contrasenaHash, String sector) {
        super(nombre, documento, correo, contrasenaHash);
        this.sector = sector;
    }

    @Override
    public Rol getRol() {
        return Rol.CIUDADANO;
    }

    /** Caso de uso "Mi perfil" (HU-02): cambia nombre, correo y sector en una sola operación. */
    public void actualizarPerfil(String nuevoNombre, String nuevoCorreo, String nuevoSector) {
        actualizarNombreYCorreo(nuevoNombre, nuevoCorreo);
        this.sector = nuevoSector;
    }

    @Override
    public void guardarEn(RegistroUsuarios registro) {
        registro.guardar(this);
    }

    /** Se usa para mostrar los horarios y puntos de recolección de su sector (HU-12, HU-13). */
    @Override
    public String getSector() { return sector; }
}
