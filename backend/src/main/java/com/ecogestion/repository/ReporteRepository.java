package com.ecogestion.repository;

import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Acceso a los reportes ({@code ReporteRepository extends MongoRepository<Reporte, String>} en la vista de desarrollo).
 *
 * <p><b>Cómo funciona:</b> es solo una interfaz. Spring Data MongoDB genera la implementación al arrancar:
 * los métodos heredados (save, findById, findAll...) y los que siguen la convención de nombres
 * ({@code findBy...}, {@code existsBy...}, {@code countBy...}) se traducen automáticamente a consultas.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> no representa nada del caso de estudio; existe para separar el acceso
 *       a la base de datos de las clases del dominio, que así no saben nada de MongoDB.</li>
 *   <li><b>Indirección:</b> los servicios hablan con esta interfaz y no con MongoDB directamente, lo que
 *       mantiene <b>bajo acoplamiento</b> y permite reemplazarla por un simulacro (mock) en las pruebas.</li>
 * </ul>
 */
public interface ReporteRepository extends MongoRepository<Reporte, String> {

    /** Bandeja de revisión del administrador (HU-05). */
    List<Reporte> findByEstado(EstadoReporte estado);

    /** Historial del ciudadano (HU-10, asociación historial). */
    List<Reporte> findByCiudadanoOrderByFechaReporteDesc(Ciudadano ciudadano);

    /** Reportes asignados a un operador que no están resueltos (HU-08). */
    List<Reporte> findByOperadorAndEstadoNot(Operador operador, EstadoReporte estado);

    long countByOperadorAndEstadoNot(Operador operador, EstadoReporte estado);

    /** Duplicados vinculados a un reporte original (HU-06, HU-11). */
    List<Reporte> findByReporteOriginal(Reporte original);

    /** Reportes abiertos del mismo tipo a menos de {@code metros} del punto, con índice 2dsphere (HU-06). */
    @Query("{ 'ubicacion': { $near: { $geometry: ?0, $maxDistance: ?1 } }, 'tipoResiduo': ?2, 'estado': { $ne: 'RESUELTO' } }")
    List<Reporte> buscarAbiertosCercanos(GeoJsonPoint punto, double metros, TipoResiduo tipo);
}
