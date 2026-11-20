package com.ecogestion.service;

import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Componente "Clasificación y priorización". Provee "Clasificar y priorizar" (a Gestión de reportes)
 * y "Consultar tipos y prioridades" (a Estadísticas e informes). Historia HU-05.
 */
@Service
public class ClasificacionService {

    /** Prioridad sugerida según el tipo de residuo: peligroso = Alta (HU-05, criterio 2). */
    public Prioridad prioridadSugerida(TipoResiduo tipo) {
        return tipo.prioridadSugerida();
    }

    /** Clasifica un reporte nuevo con su prioridad sugerida. */
    public void clasificarAutomaticamente(Reporte reporte, TipoResiduo tipo) {
        reporte.clasificar(tipo, prioridadSugerida(tipo));
    }

    /** Reclasificación manual del administrador (HU-05, criterio 3). */
    public void clasificar(Reporte reporte, TipoResiduo tipo, Prioridad prioridad) {
        reporte.clasificar(tipo, prioridad);
    }

    /** Catálogo de tipos de residuo con su prioridad sugerida. */
    public Map<TipoResiduo, Prioridad> tiposYPrioridades() {
        Map<TipoResiduo, Prioridad> catalogo = new EnumMap<>(TipoResiduo.class);
        for (TipoResiduo tipo : TipoResiduo.values()) {
            catalogo.put(tipo, tipo.prioridadSugerida());
        }
        return catalogo;
    }
}
