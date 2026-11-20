package com.ecogestion.repository;

import com.ecogestion.model.Ciudadano;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Acceso a los ciudadanos (colección ciudadanos); se usa en el registro, el inicio de sesión y el perfil.
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
public interface CiudadanoRepository extends MongoRepository<Ciudadano, String> {

    Optional<Ciudadano> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByDocumento(String documento);
}
