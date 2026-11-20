package com.ecogestion.repository;

import com.ecogestion.model.Operador;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OperadorRepository extends MongoRepository<Operador, String> {

    Optional<Operador> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByDocumento(String documento);

    List<Operador> findByDisponibleTrueOrderByNombreAsc();
}
