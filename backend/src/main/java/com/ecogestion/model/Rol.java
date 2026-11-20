package com.ecogestion.model;

/** Roles del sistema (actores de la vista de contexto). */
public enum Rol {
    CIUDADANO,
    ADMINISTRADOR,
    OPERADOR;

    /** Autoridad usada por Spring Security. */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
