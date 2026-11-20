package com.ecogestion.model;

/**
 * Tipos de residuo que el ciudadano elige al reportar (HU-03), cada uno con su prioridad sugerida (HU-05).
 *
 * <p><b>Patrón GRASP — Experto en información:</b> cada tipo conoce su prioridad sugerida y si es
 * peligroso; nadie más necesita una tabla aparte con esa información.
 */
public enum TipoResiduo {
    DOMESTICO(Prioridad.BAJA),
    ESCOMBROS(Prioridad.MEDIA),
    RECICLABLE(Prioridad.BAJA),
    VOLUMINOSO(Prioridad.MEDIA),
    PELIGROSO(Prioridad.ALTA),
    OTRO(Prioridad.MEDIA);

    private final Prioridad prioridadSugerida;

    TipoResiduo(Prioridad prioridadSugerida) {
        this.prioridadSugerida = prioridadSugerida;
    }

    public Prioridad prioridadSugerida() {
        return prioridadSugerida;
    }

    public boolean esPeligroso() {
        return this == PELIGROSO;
    }
}
