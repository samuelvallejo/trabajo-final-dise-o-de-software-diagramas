package com.ecogestion.model;

/**
 * Roles del sistema: son los tres actores de la vista de contexto.
 * Spring Security usa {@link #autoridad()} para decidir qué rutas puede usar cada uno.
 */
public enum Rol {
    CIUDADANO,
    ADMINISTRADOR,
    OPERADOR;

    /** Nombre de la autoridad en Spring Security (por convención lleva el prefijo ROLE_). */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
