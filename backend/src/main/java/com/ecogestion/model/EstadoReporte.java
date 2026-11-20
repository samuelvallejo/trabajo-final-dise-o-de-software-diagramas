package com.ecogestion.model;

/**
 * Estados por los que pasa un reporte, en este orden obligatorio (HU-09):
 * PENDIENTE → ASIGNADO → EN_PROCESO → RESUELTO.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> el propio estado sabe cuál es el siguiente
 * permitido, así la regla del flujo está escrita en un solo lugar.
 */
public enum EstadoReporte {
    PENDIENTE,
    ASIGNADO,
    EN_PROCESO,
    RESUELTO;

    /** Solo se permite avanzar al estado inmediatamente siguiente (no se saltan ni se retroceden estados). */
    public boolean puedeAvanzarA(EstadoReporte siguiente) {
        return siguiente != null && siguiente.ordinal() == ordinal() + 1;
    }
}
