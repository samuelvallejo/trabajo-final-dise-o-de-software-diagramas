package com.ecogestion.model;

/**
 * Estados del reporte. Flujo obligatorio (HU-09): Pendiente → Asignado → En proceso → Resuelto.
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

    public boolean estaAbierto() {
        return this != RESUELTO;
    }
}
