package com.ecogestion.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Operador de limpieza que atiende los reportes que le asignan (colección {@code operadores}).
 * Lo crea un administrador; no se registra por su cuenta.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> sobrescribe {@link #getRol()} para identificarse como OPERADOR.</li>
 *   <li><b>Experto en información:</b> el operador sabe si está disponible y es quien cambia ese dato
 *       (solo se asignan reportes a operadores disponibles, HU-07).</li>
 * </ul>
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

    /** El operador deja de recibir asignaciones nuevas (por ejemplo, está de vacaciones). */
    public void marcarOcupado() {
        this.disponible = false;
    }

    public void marcarDisponible() {
        this.disponible = true;
    }

    public boolean isDisponible() { return disponible; }
}
