package com.ecogestion.model;

/**
 * Prioridad de atención de un reporte (HU-05). El número de orden sirve para ordenar la bandeja:
 * primero se atiende lo de prioridad ALTA.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> la prioridad sabe su propio orden de atención.
 */
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
