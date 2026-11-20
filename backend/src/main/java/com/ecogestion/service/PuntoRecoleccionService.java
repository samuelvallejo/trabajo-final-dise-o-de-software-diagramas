package com.ecogestion.service;

import com.ecogestion.dto.RecoleccionDto.PuntoRequest;
import com.ecogestion.dto.RecoleccionDto.PuntoResponse;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.model.MaterialAceptado;
import com.ecogestion.model.PuntoRecoleccion;
import com.ecogestion.repository.PuntoRecoleccionRepository;
import com.ecogestion.util.Geo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lógica del componente "Puntos de recolección" (HU-13): consultar los puntos autorizados con filtros
 * y, para el administrador, crearlos, editarlos y desactivarlos.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Creador:</b> crea los {@link PuntoRecoleccion} con los datos que envía el administrador.</li>
 *   <li><b>Bajo acoplamiento:</b> para validar o identificar el sector usa el servicio "Consultar sector"
 *       de {@link HorarioRutaService} en vez de leer la colección de sectores.</li>
 *   <li><b>Experto en información:</b> cada punto decide si recibe un material ({@code recibeMaterial}).</li>
 * </ul>
 */
@Service
public class PuntoRecoleccionService {

    private final PuntoRecoleccionRepository puntoRepository;
    private final HorarioRutaService horarioRutaService;

    public PuntoRecoleccionService(PuntoRecoleccionRepository puntoRepository, HorarioRutaService horarioRutaService) {
        this.puntoRepository = puntoRepository;
        this.horarioRutaService = horarioRutaService;
    }

    /**
     * Puntos activos filtrados por material y sector; si se envía la ubicación del ciudadano, se ordenan
     * por cercanía (HU-13, criterios 1, 2 y 4).
     */
    public List<PuntoResponse> listar(MaterialAceptado material, String sector, Double latitud, Double longitud) {
        String nombreSector = sector == null || sector.isBlank() ? null : horarioRutaService.consultarSector(sector).getNombre();
        Stream<PuntoRecoleccion> puntos = puntoRepository.findByActivoTrue().stream()
                .filter(p -> material == null || p.recibeMaterial(material))
                .filter(p -> nombreSector == null || nombreSector.equalsIgnoreCase(p.getSector()));
        if (latitud != null && longitud != null) {
            return puntos.map(p -> PuntoResponse.de(p, Geo.distanciaMetros(latitud, longitud, p.getLatitud(), p.getLongitud())))
                    .sorted(Comparator.comparing(PuntoResponse::distanciaMetros))
                    .toList();
        }
        return puntos.map(p -> PuntoResponse.de(p, null))
                .sorted(Comparator.comparing(PuntoResponse::nombre))
                .toList();
    }

    public List<PuntoResponse> listarTodos() {
        return puntoRepository.findAll().stream().map(p -> PuntoResponse.de(p, null)).toList();
    }

    public PuntoResponse crear(PuntoRequest datos) {
        PuntoRecoleccion punto = new PuntoRecoleccion(datos.nombre(), datos.direccion(), sectorValido(datos),
                datos.latitud(), datos.longitud(), datos.materiales(), datos.horarioAtencion());
        return PuntoResponse.de(puntoRepository.save(punto), null);
    }

    public PuntoResponse actualizar(String puntoId, PuntoRequest datos) {
        PuntoRecoleccion punto = buscar(puntoId);
        punto.actualizar(datos.nombre(), datos.direccion(), sectorValido(datos), datos.latitud(), datos.longitud(),
                datos.materiales(), datos.horarioAtencion());
        return PuntoResponse.de(puntoRepository.save(punto), null);
    }

    public PuntoResponse cambiarActivacion(String puntoId, boolean activo) {
        PuntoRecoleccion punto = buscar(puntoId);
        if (activo) {
            punto.activar();
        } else {
            punto.desactivar();
        }
        return PuntoResponse.de(puntoRepository.save(punto), null);
    }

    /** Si no se indica sector, se identifica a partir de la ubicación del punto. */
    private String sectorValido(PuntoRequest datos) {
        if (datos.sector() != null && !datos.sector().isBlank()) {
            return horarioRutaService.consultarSector(datos.sector()).getNombre();
        }
        return horarioRutaService.identificarSector(datos.latitud(), datos.longitud()).map(s -> s.getNombre()).orElse(null);
    }

    private PuntoRecoleccion buscar(String puntoId) {
        return puntoRepository.findById(puntoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Punto de recolección", puntoId));
    }
}
