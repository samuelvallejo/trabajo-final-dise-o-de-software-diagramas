package com.ecogestion.repository;

import com.ecogestion.model.Sector;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los sectores de la ciudad y sus barrios (HU-12).
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
public interface SectorRepository extends MongoRepository<Sector, String> {

    Optional<Sector> findByNombreIgnoreCase(String nombre);

    List<Sector> findAllByOrderByNombreAsc();
}
