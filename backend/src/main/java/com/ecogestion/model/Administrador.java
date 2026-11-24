package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Administrador de la entidad ambiental (colección {@code administradores}): revisa, clasifica y asigna
 * reportes, y mantiene horarios, puntos de recolección y contenido educativo.
 * Cumple el papel de {@code GestorReportes} del modelo de clases inicial (vista lógica).
 *
 * <p><b>Patrón GRASP — Polimorfismo:</b> sobrescribe {@link #getRol()} para identificarse como ADMINISTRADOR.
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

    @Override
    public void guardarEn(RegistroUsuarios registro) {
        registro.guardar(this);
    }
}
