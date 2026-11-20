package com.ecogestion.model;

/**
 * Tipos de residuo del reporte (HU-03) y su prioridad sugerida (HU-05:
 * los residuos peligrosos tienen más prioridad que los domésticos).
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
