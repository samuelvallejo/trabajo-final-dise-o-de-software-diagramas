package com.ecogestion.repository;

import com.ecogestion.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByDestinatarioIdOrderByFechaCreacionDesc(String destinatarioId);
}
