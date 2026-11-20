package com.ecogestion.repository;

import com.ecogestion.model.RutaRecoleccion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RutaRecoleccionRepository extends MongoRepository<RutaRecoleccion, String> {
}
