package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.ReporteDto.PosibleDuplicadoResponse;
import com.ecogestion.dto.ReporteDto.ReporteRequest;
import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.dto.UsuarioDto.OperadorResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.excepcion.ReglaNegocioException;
import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.integracion.mapas.ServicioMapas;
import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.Rol;
import com.ecogestion.model.Sector;
import com.ecogestion.model.TipoResiduo;
import com.ecogestion.repository.CiudadanoRepository;
import com.ecogestion.repository.OperadorRepository;
import com.ecogestion.repository.ReporteRepository;
import com.ecogestion.util.Geo;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code «Service» ReporteService} de la vista de desarrollo. Implementa el componente "Gestión de
 * reportes" (HU-03, HU-04, HU-06, HU-10) y la asignación a operadores (HU-05, HU-07).
 */
@Service
public class ReporteService {

    private final CiudadanoRepository ciudadanoRepository;
    private final OperadorRepository operadorRepository;
    private final ReporteRepository reporteRepository;
    private final ClasificacionService clasificacionService;
    private final HorarioRutaService horarioRutaService;
    private final NotificacionService notificacionService;
    private final ServicioMapas servicioMapas;
    private final AlmacenamientoImagenes almacenamientoImagenes;
    private final EcoGestionProperties propiedades;

    public ReporteService(CiudadanoRepository ciudadanoRepository, OperadorRepository operadorRepository,
                          ReporteRepository reporteRepository, ClasificacionService clasificacionService,
                          HorarioRutaService horarioRutaService, NotificacionService notificacionService,
                          ServicioMapas servicioMapas, AlmacenamientoImagenes almacenamientoImagenes,
                          EcoGestionProperties propiedades) {
        this.ciudadanoRepository = ciudadanoRepository;
        this.operadorRepository = operadorRepository;
        this.reporteRepository = reporteRepository;
        this.clasificacionService = clasificacionService;
        this.horarioRutaService = horarioRutaService;
        this.notificacionService = notificacionService;
        this.servicioMapas = servicioMapas;
        this.almacenamientoImagenes = almacenamientoImagenes;
        this.propiedades = propiedades;
    }

    // ------------------------------------------------------------------ HU-03 y HU-04: registrar reporte

    public ReporteResponse registrarReporte(ReporteRequest request, List<MultipartFile> fotos, String ciudadanoId) {
        Ciudadano ciudadano = ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudadano", ciudadanoId));
        double latitud = request.latitud();
        double longitud = request.longitud();
        if (!propiedades.municipio().contiene(latitud, longitud)) {
            throw new DatosInvalidosException("La ubicación está fuera del municipio de Pasto");
        }
        Reporte reporte = new Reporte(LocalDateTime.now(), ciudadano, request.descripcion());
        clasificacionService.clasificarAutomaticamente(reporte, request.tipoResiduo());
        String direccion = servicioMapas.obtenerDireccion(latitud, longitud).orElse(null);
        String sector = horarioRutaService.identificarSector(latitud, longitud).map(Sector::getNombre).orElse(null);
        reporte.registrarUbicacion(latitud, longitud, direccion, sector);
        reporte.adjuntarFotos(almacenamientoImagenes.guardar(fotos));
        return ReporteResponse.de(reporteRepository.save(reporte));
    }

    // ------------------------------------------------------------------ consultas (HU-05, HU-10)

    /**
     * El ciudadano ve su historial (HU-10); el administrador ve la bandeja de revisión ordenada por
     * prioridad y fecha, con filtros por estado, tipo, prioridad y sector (HU-05).
     */
    public List<ReporteResponse> listarReportes(String usuarioId, Rol rol, FiltroReportes filtro) {
        Stream<Reporte> reportes = switch (rol) {
            case CIUDADANO -> reporteRepository.findByCiudadanoOrderByFechaReporteDesc(buscarCiudadano(usuarioId)).stream();
            case ADMINISTRADOR -> (filtro.estado() == null
                    ? reporteRepository.findByEstado(EstadoReporte.PENDIENTE).stream().filter(Reporte::estaPendiente)
                    : reporteRepository.findByEstado(filtro.estado()).stream())
                    .sorted(Comparator.comparing((Reporte r) -> r.getPrioridad().orden())
                            .thenComparing(Reporte::getFechaReporte));
            case OPERADOR -> reportesDeOperador(usuarioId, "prioridad").stream();
        };
        return reportes.filter(filtro::cumple).map(ReporteResponse::de).toList();
    }

    public ReporteResponse obtenerReporte(String id, String usuarioId, Rol rol) {
        Reporte reporte = buscar(id);
        boolean permitido = switch (rol) {
            case ADMINISTRADOR -> true;
            case CIUDADANO -> reporte.esDelCiudadano(usuarioId);
            case OPERADOR -> reporte.estaAsignadoA(usuarioId);
        };
        if (!permitido) {
            throw new RecursoNoEncontradoException("Reporte", id);
        }
        return ReporteResponse.de(reporte);
    }

    /** "Consultar reportes" (lo consume Estadísticas e informes). */
    public List<Reporte> consultarReportes() {
        return reporteRepository.findAll();
    }

    // ------------------------------------------------------------------ HU-05: clasificación manual

    public ReporteResponse clasificarReporte(String id, TipoResiduo tipo, Prioridad prioridad) {
        Reporte reporte = buscar(id);
        clasificacionService.clasificar(reporte, tipo, prioridad);
        return ReporteResponse.de(reporteRepository.save(reporte));
    }

    // ------------------------------------------------------------------ HU-06: duplicados

    public List<PosibleDuplicadoResponse> posiblesDuplicados(String id) {
        Reporte reporte = buscar(id);
        double radio = propiedades.duplicados().radioMetros();
        return reporteRepository.buscarAbiertosCercanos(reporte.getUbicacion(), radio, reporte.getTipoResiduo()).stream()
                .filter(r -> !r.getId().equals(reporte.getId()) && !r.esDuplicado())
                .map(r -> new PosibleDuplicadoResponse(ReporteResponse.de(r),
                        Geo.distanciaMetros(reporte.getLatitud(), reporte.getLongitud(), r.getLatitud(), r.getLongitud())))
                .toList();
    }

    public ReporteResponse marcarComoDuplicado(String id, String reporteOriginalId) {
        Reporte reporte = buscar(id);
        reporte.marcarComoDuplicado(buscar(reporteOriginalId));
        return ReporteResponse.de(reporteRepository.save(reporte));
    }

    public ReporteResponse desvincularDuplicado(String id) {
        Reporte reporte = buscar(id);
        reporte.desvincularDuplicado();
        return ReporteResponse.de(reporteRepository.save(reporte));
    }

    // ------------------------------------------------------------------ HU-07: asignación sin doble asignación

    public ReporteResponse asignarReporte(String id, String operadorId, boolean reasignar) {
        Reporte reporte = buscar(id);
        Operador operador = operadorRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador", operadorId));
        validarDisponibilidad(operador);
        EstadoReporte estadoAnterior = reporte.getEstado();
        if (reasignar) {
            reporte.reasignar(operador);
        } else {
            reporte.asignar(operador);
        }
        Reporte guardado = guardarConControlDeConcurrencia(reporte);
        if (guardado.getEstado() != estadoAnterior) {
            notificacionService.notificarCambioEstado(guardado);
        }
        return ReporteResponse.de(guardado);
    }

    /** Operadores disponibles con su número de reportes sin resolver (HU-07, criterio 4). */
    public List<OperadorResponse> operadoresParaAsignar() {
        return operadorRepository.findByDisponibleTrueOrderByNombreAsc().stream()
                .map(o -> new OperadorResponse(o.getId(), o.getNombre(), o.getCorreo(), o.isDisponible(),
                        reporteRepository.countByOperadorAndEstadoNot(o, EstadoReporte.RESUELTO)))
                .toList();
    }

    private void validarDisponibilidad(Operador operador) {
        if (!operador.isDisponible()) {
            throw new ReglaNegocioException("El operador " + operador.getNombre() + " no está disponible");
        }
    }

    // ------------------------------------------------------------------ apoyo a Asignación y atención (HU-08, HU-09)

    /** Reportes sin resolver de un operador, ordenados por prioridad o por fecha (HU-08, criterio 4). */
    public List<Reporte> reportesDeOperador(String operadorId, String orden) {
        Operador operador = operadorRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador", operadorId));
        Comparator<Reporte> comparador = "fecha".equalsIgnoreCase(orden)
                ? Comparator.comparing(Reporte::getFechaReporte)
                : Comparator.comparing((Reporte r) -> r.getPrioridad().orden()).thenComparing(Reporte::getFechaReporte);
        return reporteRepository.findByOperadorAndEstadoNot(operador, EstadoReporte.RESUELTO).stream()
                .sorted(comparador).toList();
    }

    public Reporte buscarParaOperador(String id, String operadorId) {
        Reporte reporte = buscar(id);
        if (!reporte.estaAsignadoA(operadorId)) {
            throw new RecursoNoEncontradoException("Reporte asignado", id);
        }
        return reporte;
    }

    /** "Consultar y actualizar estado del reporte": guarda el cambio y notifica al ciudadano. */
    public Reporte actualizarEstado(Reporte reporte) {
        Reporte guardado = guardarConControlDeConcurrencia(reporte);
        notificacionService.notificarCambioEstado(guardado);
        return guardado;
    }

    // ------------------------------------------------------------------ apoyo

    private Reporte buscar(String id) {
        return reporteRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reporte", id));
    }

    private Ciudadano buscarCiudadano(String ciudadanoId) {
        return ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudadano", ciudadanoId));
    }

    private Reporte guardarConControlDeConcurrencia(Reporte reporte) {
        try {
            return reporteRepository.save(reporte);
        } catch (OptimisticLockingFailureException e) {
            throw new ReglaNegocioException("Otro usuario modificó el reporte al mismo tiempo; recargue e intente de nuevo");
        }
    }

    /** Filtros opcionales de la bandeja y del historial. */
    public record FiltroReportes(EstadoReporte estado, TipoResiduo tipoResiduo, Prioridad prioridad, String sector) {
        boolean cumple(Reporte r) {
            return (estado == null || r.estadoVisible() == estado)
                    && (tipoResiduo == null || r.getTipoResiduo() == tipoResiduo)
                    && (prioridad == null || r.getPrioridad() == prioridad)
                    && (sector == null || sector.equalsIgnoreCase(r.getSector()));
        }
    }
}
