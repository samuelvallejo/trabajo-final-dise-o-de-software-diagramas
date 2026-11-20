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
 * Repositorio de reportes ({@code ReporteRepository extends MongoRepository<Reporte, String>} en la
 * vista de desarrollo).
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
