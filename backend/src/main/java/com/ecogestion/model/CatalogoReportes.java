package com.ecogestion.model;

import java.util.List;

/**
 * Consultas de reportes que necesita cada rol para armar su lista (la implementa {@code ReporteService}).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Indirección y bajo acoplamiento:</b> {@link Rol} decide <i>qué</i> lista le corresponde a cada
 *       usuario, pero no sabe <i>cómo</i> se busca en MongoDB; solo conoce esta interfaz.</li>
 *   <li><b>Variaciones protegidas:</b> se puede cambiar la forma de consultar sin tocar las reglas de los roles.</li>
 * </ul>
 */
public interface CatalogoReportes {

    /** Reportes de un ciudadano, del más reciente al más antiguo (HU-10). */
    List<Reporte> historialDelCiudadano(String ciudadanoId);

    /** Bandeja del administrador: sin estado, los pendientes que no son duplicados (HU-05). */
    List<Reporte> bandejaDeRevision(EstadoReporte estado);

    /** Reportes sin resolver asignados a un operador, por prioridad (HU-08). */
    List<Reporte> pendientesDelOperador(String operadorId);
}
