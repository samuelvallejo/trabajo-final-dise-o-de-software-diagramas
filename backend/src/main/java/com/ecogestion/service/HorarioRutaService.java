package com.ecogestion.service;

import com.ecogestion.dto.RecoleccionDto.HorarioRequest;
import com.ecogestion.dto.RecoleccionDto.HorariosSectorResponse;
import com.ecogestion.dto.RecoleccionDto.RutaRequest;
import com.ecogestion.dto.RecoleccionDto.RutaResponse;
import com.ecogestion.dto.RecoleccionDto.SectorResponse;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.model.HorarioRecoleccion;
import com.ecogestion.model.RutaRecoleccion;
import com.ecogestion.model.Sector;
import com.ecogestion.repository.RutaRecoleccionRepository;
import com.ecogestion.repository.SectorRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Componente "Horarios y rutas". Provee "Identificar sector" (a Gestión de reportes) y
 * "Consultar sector" (a Puntos de recolección). Historia HU-12.
 */
@Service
public class HorarioRutaService {

    private final SectorRepository sectorRepository;
    private final RutaRecoleccionRepository rutaRepository;
    private final MongoTemplate mongoTemplate;

    public HorarioRutaService(SectorRepository sectorRepository, RutaRecoleccionRepository rutaRepository,
                              MongoTemplate mongoTemplate) {
        this.sectorRepository = sectorRepository;
        this.rutaRepository = rutaRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /** "Identificar sector": sector cuyo límite contiene el punto (HU-04, criterio 3). */
    public Optional<Sector> identificarSector(double latitud, double longitud) {
        Query consulta = Query.query(Criteria.where("limite").intersects(new GeoJsonPoint(longitud, latitud)));
        return Optional.ofNullable(mongoTemplate.findOne(consulta, Sector.class));
    }

    /** "Consultar sector" por nombre. */
    public Sector consultarSector(String nombre) {
        return sectorRepository.findByNombreIgnoreCase(nombre.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sector", nombre));
    }

    public List<SectorResponse> listarSectores() {
        return sectorRepository.findAllByOrderByNombreAsc().stream().map(SectorResponse::de).toList();
    }

    /** Horarios y rutas de un sector o del sector que contiene un barrio (HU-12, criterios 1 y 2). */
    public HorariosSectorResponse horariosDe(String sectorOBarrio) {
        Sector sector = sectorRepository.findByNombreIgnoreCase(sectorOBarrio.trim())
                .or(() -> sectorRepository.findAll().stream().filter(s -> s.incluyeBarrio(sectorOBarrio.trim())).findFirst())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sector o barrio", sectorOBarrio));
        List<RutaResponse> rutas = rutaRepository.findAll().stream()
                .filter(r -> r.cubreSector(sector.getNombre()))
                .map(RutaResponse::de)
                .toList();
        return new HorariosSectorResponse(SectorResponse.de(sector), rutas);
    }

    public List<RutaResponse> listarRutas() {
        return rutaRepository.findAll().stream().map(RutaResponse::de).toList();
    }

    /** El administrador crea una ruta con sus sectores y horarios (HU-12, criterio 3). */
    public RutaResponse crearRuta(RutaRequest datos) {
        RutaRecoleccion ruta = new RutaRecoleccion(datos.nombre(), sectoresValidados(datos.sectores()),
                horarios(datos.horarios()), datos.recorrido());
        return RutaResponse.de(rutaRepository.save(ruta));
    }

    public RutaResponse actualizarRuta(String rutaId, RutaRequest datos) {
        RutaRecoleccion ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta", rutaId));
        ruta.actualizar(datos.nombre(), sectoresValidados(datos.sectores()), horarios(datos.horarios()), datos.recorrido());
        return RutaResponse.de(rutaRepository.save(ruta));
    }

    private List<String> sectoresValidados(List<String> nombres) {
        return nombres.stream().map(n -> consultarSector(n).getNombre()).distinct().toList();
    }

    private static List<HorarioRecoleccion> horarios(List<HorarioRequest> horarios) {
        return horarios.stream().map(HorarioRequest::aDominio).toList();
    }
}
