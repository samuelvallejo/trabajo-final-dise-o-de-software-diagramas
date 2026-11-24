package com.ecogestion.model;

/**
 * Estados por los que pasa un reporte, en este orden obligatorio (HU-09):
 * PENDIENTE → ASIGNADO → EN_PROCESO → RESUELTO.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> cada estado sabe cuál es el siguiente permitido, cómo
 * se llama para mostrarlo al usuario y si su llegada se le notifica al ciudadano. Así ninguna otra clase
 * necesita un {@code switch} sobre el estado.
 */
public enum EstadoReporte {
    PENDIENTE("Pendiente", false),
    ASIGNADO("Asignado", true),
    EN_PROCESO("En proceso", true),
    RESUELTO("Resuelto", true);

    private final String etiqueta;
    private final boolean seNotifica;

    EstadoReporte(String etiqueta, boolean seNotifica) {
        this.etiqueta = etiqueta;
        this.seNotifica = seNotifica;
    }

    /** Solo se permite avanzar al estado inmediatamente siguiente (no se saltan ni se retroceden estados). */
    public boolean puedeAvanzarA(EstadoReporte siguiente) {
        return siguiente != null && siguiente.ordinal() == ordinal() + 1;
    }

    /** El caso de estudio pide avisar cuando el reporte se asigna, empieza a atenderse o se resuelve (HU-11). */
    public boolean seNotifica() {
        return seNotifica;
    }

    /** Nombre legible del estado, por ejemplo "En proceso". */
    public String etiqueta() {
        return etiqueta;
    }
}
