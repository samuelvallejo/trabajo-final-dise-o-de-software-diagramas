package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Administrador de la entidad ambiental: revisa, clasifica y asigna reportes, y mantiene horarios,
 * puntos de recolección y contenido educativo. Implementa el rol de {@code GestorReportes} del modelo inicial.
 */
@Document("administradores")
public class Administrador extends Usuario {

    protected Administrador() {
    }

    public Administrador(String nombre, String documento, String correo, String contrasenaHash) {
        super(nombre, documento, correo, contrasenaHash);
    }

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }
}
