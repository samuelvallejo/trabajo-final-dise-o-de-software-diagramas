package com.ecogestion.model;

/** Prioridad de atención de un reporte (HU-05). */
public enum Prioridad {
    ALTA(1),
    MEDIA(2),
    BAJA(3);

    private final int orden;

    Prioridad(int orden) {
        this.orden = orden;
    }

    /** Menor valor = se atiende primero. */
    public int orden() {
        return orden;
    }
}
