package com.ecogestion.service;

import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lógica del componente "Clasificación y priorización" (HU-05). Provee "Clasificar y priorizar" a
 * Gestión de reportes y "Consultar tipos y prioridades" a Estadísticas e informes.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Alta cohesión:</b> concentra en un solo lugar cómo se clasifica un reporte; si mañana la
 *       prioridad depende también del sector o de la hora, solo cambia esta clase.</li>
 *   <li><b>Variaciones protegidas:</b> el resto del sistema pide "clasifica este reporte" sin saber cómo
 *       se calcula la prioridad.</li>
 *   <li><b>Experto en información:</b> la prioridad sugerida la sabe cada {@link TipoResiduo} y la regla
 *       "peligroso = Alta" la valida el {@link Reporte}.</li>
 * </ul>
 */
@Service
public class ClasificacionService {

    /** Clasifica un reporte nuevo con la prioridad sugerida de su tipo (peligroso = Alta). */
    public void clasificarAutomaticamente(Reporte reporte, TipoResiduo tipo) {
        reporte.clasificar(tipo, tipo.prioridadSugerida());
    }

    /** Reclasificación manual del administrador (HU-05, criterio 3). */
    public void clasificar(Reporte reporte, TipoResiduo tipo, Prioridad prioridad) {
        reporte.clasificar(tipo, prioridad);
    }

    /** Catálogo de tipos de residuo con su prioridad sugerida (se muestra en el formulario del ciudadano). */
    public Map<TipoResiduo, Prioridad> tiposYPrioridades() {
        Map<TipoResiduo, Prioridad> catalogo = new EnumMap<>(TipoResiduo.class);
        for (TipoResiduo tipo : TipoResiduo.values()) {
            catalogo.put(tipo, tipo.prioridadSugerida());
        }
        return catalogo;
    }
}
