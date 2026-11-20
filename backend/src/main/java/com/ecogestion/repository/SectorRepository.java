package com.ecogestion.repository;

import com.ecogestion.model.Sector;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SectorRepository extends MongoRepository<Sector, String> {

    Optional<Sector> findByNombreIgnoreCase(String nombre);

    List<Sector> findAllByOrderByNombreAsc();
}
