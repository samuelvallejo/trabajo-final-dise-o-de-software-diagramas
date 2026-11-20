package com.ecogestion.repository;

import com.ecogestion.model.ContenidoEducativo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContenidoEducativoRepository extends MongoRepository<ContenidoEducativo, String> {

    List<ContenidoEducativo> findByPublicadoTrueOrderByFechaPublicacionDesc();
}
