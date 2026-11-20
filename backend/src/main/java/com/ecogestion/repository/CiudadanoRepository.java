package com.ecogestion.repository;

import com.ecogestion.model.Ciudadano;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CiudadanoRepository extends MongoRepository<Ciudadano, String> {

    Optional<Ciudadano> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByDocumento(String documento);
}
