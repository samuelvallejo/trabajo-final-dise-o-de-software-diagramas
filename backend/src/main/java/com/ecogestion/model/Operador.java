package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Operador de limpieza que atiende los reportes asignados (vistas lógica y de desarrollo:
 * {@code «Document» Operador} con id, nombre y disponible). Lo crea un administrador (HU-01, nota).
 */
@Document("operadores")
public class Operador extends Usuario {

    private boolean disponible = true;

    protected Operador() {
    }

    public Operador(String nombre, String documento, String correo, String contrasenaHash) {
        super(nombre, documento, correo, contrasenaHash);
    }

    @Override
    public Rol getRol() {
        return Rol.OPERADOR;
    }

    /** El operador deja de recibir nuevas asignaciones (HU-07: solo se asigna a operadores disponibles). */
    public void marcarOcupado() {
        this.disponible = false;
    }

    public void marcarDisponible() {
        this.disponible = true;
    }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean valor) { this.disponible = valor; }
}
