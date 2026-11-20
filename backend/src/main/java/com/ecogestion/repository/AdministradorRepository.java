package com.ecogestion.repository;

import com.ecogestion.model.Administrador;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdministradorRepository extends MongoRepository<Administrador, String> {

    Optional<Administrador> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
