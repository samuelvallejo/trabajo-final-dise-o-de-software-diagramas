package com.ecogestion.repository;

import com.ecogestion.model.PuntoRecoleccion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PuntoRecoleccionRepository extends MongoRepository<PuntoRecoleccion, String> {

    List<PuntoRecoleccion> findByActivoTrue();
}
