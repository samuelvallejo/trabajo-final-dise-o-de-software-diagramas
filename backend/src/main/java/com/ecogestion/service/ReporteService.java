package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.ReporteDto.PosibleDuplicadoResponse;
import com.ecogestion.dto.ReporteDto.ReporteRequest;
import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.excepcion.ReglaNegocioException;
import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.integracion.mapas.ServicioMapas;
import com.ecogestion.model.CatalogoReportes;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Lógica de aplicación del componente "Gestión de reportes" ({@code «Service» ReporteService} de la vista
 * de desarrollo): registrar, consultar, clasificar, detectar duplicados y asignar reportes
 * (HU-03 a HU-07 y HU-10).
 *
 * <p><b>Cómo funciona:</b> cada método público es un caso de uso. El patrón es siempre el mismo:
 * 1) buscar los objetos en los repositorios, 2) pedirle al objeto de dominio que haga el cambio
 * (por ejemplo {@code reporte.asignar(operador)}), 3) guardar y 4) devolver un DTO de respuesta.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Creador:</b> crea los objetos {@link Reporte} porque tiene los datos para inicializarlos
 *       (el ciudadano autenticado, la descripción y la ubicación) y es quien los registra.</li>
 *   <li><b>Fabricación pura:</b> no es un concepto del caso de estudio; existe para coordinar
 *       repositorios, mapas, almacenamiento de fotos y notificaciones sin cargar esas tareas al
 *       {@link Reporte}.</li>
 *   <li><b>Alta cohesión:</b> solo atiende operaciones de reportes; los usuarios, horarios o estadísticas
 *       tienen su propio servicio.</li>
 *   <li><b>Bajo acoplamiento / Variaciones protegidas:</b> depende de interfaces
 *       ({@link ServicioMapas}, {@link AlmacenamientoImagenes}); cambiar OpenStreetMap o GridFS por otro
 *       proveedor no obliga a modificar esta clase.</li>
 * </ul>
 * Las reglas de negocio <b>no</b> están aquí: viven en {@link Reporte} (Experto en información).
 */
@Service
public class ReporteService implements CatalogoReportes {

    /** Orden de atención: primero prioridad ALTA y, a igual prioridad, el reporte más antiguo. */
    private static final Comparator<Reporte> POR_PRIORIDAD_Y_FECHA =
            Comparator.comparing((Reporte r) -> r.getPrioridad().orden()).thenComparing(Reporte::getFechaReporte);

    private final CiudadanoRepository ciudadanoRepository;
    private final OperadorRepository operadorRepository;
    private final ReporteRepository reporteRepository;
    private final ClasificacionService clasificacionService;
    private final HorarioRutaService horarioRutaService;
    private final NotificacionService notificacionService;
    private final ServicioMapas servicioMapas;
    private final AlmacenamientoImagenes almacenamientoImagenes;
    private final EcoGestionProperties propiedades;

    /** Spring inyecta todas las dependencias por el constructor (inyección de dependencias). */
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

    // ============================================================ HU-03 y HU-04: registrar un reporte

    /**
     * Crea un reporte nuevo del ciudadano autenticado.
     * Pasos: validar que la ubicación esté en Pasto → crear el reporte → clasificarlo con la prioridad
     * sugerida → calcular dirección y sector → guardar las fotos → guardar el reporte.
     */
    public ReporteResponse registrarReporte(ReporteRequest request, List<MultipartFile> fotos, String ciudadanoId) {
        Ciudadano ciudadano = buscarCiudadano(ciudadanoId);
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

    // ============================================================ HU-05 y HU-10: consultar reportes

    /**
     * Lista los reportes que le corresponden a quien pregunta y aplica los filtros opcionales.
     * Qué lista ve cada uno lo decide su {@link Rol} (Polimorfismo): el ciudadano su historial, el
     * administrador la bandeja de revisión y el operador sus asignaciones. Este servicio solo le ofrece
     * las consultas a través de la interfaz {@link CatalogoReportes}.
     */
    public List<ReporteResponse> listarReportes(String usuarioId, Rol rol, FiltroReportes filtro) {
        return rol.reportesVisibles(this, usuarioId, filtro.estado()).stream()
                .filter(filtro::cumple)
                .map(ReporteResponse::de)
                .toList();
    }

    /** Devuelve un reporte solo si el rol de quien pregunta le permite verlo; si no, responde "no encontrado". */
    public ReporteResponse obtenerReporte(String id, String usuarioId, Rol rol) {
        Reporte reporte = buscar(id);
        if (!rol.puedeVer(reporte, usuarioId)) {
            throw new RecursoNoEncontradoException("Reporte", id);
        }
        return ReporteResponse.de(reporte);
    }

    // ============================================================ CatalogoReportes (consultas por rol)

    @Override
    public List<Reporte> historialDelCiudadano(String ciudadanoId) {
        return reporteRepository.findByCiudadanoOrderByFechaReporteDesc(buscarCiudadano(ciudadanoId));
    }

    /** Sin estado: los pendientes que no son duplicados. Siempre ordenados por prioridad y fecha. */
    @Override
    public List<Reporte> bandejaDeRevision(EstadoReporte estado) {
        List<Reporte> reportes = estado == null
                ? reporteRepository.findByEstado(EstadoReporte.PENDIENTE).stream().filter(Reporte::estaPendiente).toList()
                : reporteRepository.findByEstado(estado);
        return reportes.stream().sorted(POR_PRIORIDAD_Y_FECHA).toList();
    }

    @Override
    public List<Reporte> pendientesDelOperador(String operadorId) {
        return reportesDeOperador(operadorId, "prioridad");
    }

    /** Servicio "Consultar reportes" que usa el componente Estadísticas e informes. */
    public List<Reporte> consultarReportes() {
        return reporteRepository.findAll();
    }

    // ============================================================ HU-05: clasificación manual

    public ReporteResponse clasificarReporte(String id, TipoResiduo tipo, Prioridad prioridad) {
        Reporte reporte = buscar(id);
        clasificacionService.clasificar(reporte, tipo, prioridad);
        return ReporteResponse.de(reporteRepository.save(reporte));
    }

    // ============================================================ HU-06: duplicados

    /**
     * Busca reportes abiertos del mismo tipo a menos de 50 metros (radio configurable) usando el índice
     * geográfico de MongoDB, y devuelve cada uno con su distancia.
     */
    public List<PosibleDuplicadoResponse> posiblesDuplicados(String id) {
        Reporte reporte = buscar(id);
        double radio = propiedades.duplicados().radioMetros();
        return reporteRepository.buscarAbiertosCercanos(reporte.getUbicacion(), radio, reporte.getTipoResiduo()).stream()
                .filter(cercano -> !cercano.getId().equals(reporte.getId()) && !cercano.esDuplicado())
                .map(cercano -> new PosibleDuplicadoResponse(ReporteResponse.de(cercano), reporte.distanciaMetrosA(cercano)))
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

    // ============================================================ HU-07: asignar sin doble asignación

    /**
     * Asigna (o reasigna) el reporte a un operador disponible y avisa al ciudadano.
     * La doble asignación se evita en dos niveles: {@link Reporte#asignar} rechaza un reporte que ya tiene
     * operador, y el campo {@code @Version} rechaza dos guardados simultáneos del mismo reporte.
     */
    public ReporteResponse asignarReporte(String id, String operadorId, boolean reasignar) {
        Reporte reporte = buscar(id);
        Operador operador = buscarOperador(operadorId);
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

    /** Carga de trabajo de un operador: cuántos reportes tiene sin resolver (HU-07, criterio 4). */
    public long contarSinResolver(Operador operador) {
        return reporteRepository.countByOperadorAndEstadoNot(operador, EstadoReporte.RESUELTO);
    }

    // ============================================================ HU-08 y HU-09: apoyo a AtencionService

    /** Reportes sin resolver de un operador, ordenados por prioridad (por defecto) o por fecha. */
    public List<Reporte> reportesDeOperador(String operadorId, String orden) {
        Comparator<Reporte> comparador = "fecha".equalsIgnoreCase(orden)
                ? Comparator.comparing(Reporte::getFechaReporte)
                : POR_PRIORIDAD_Y_FECHA;
        return reporteRepository.findByOperadorAndEstadoNot(buscarOperador(operadorId), EstadoReporte.RESUELTO).stream()
                .sorted(comparador)
                .toList();
    }

    /** Busca un reporte verificando que esté asignado a ese operador. */
    public Reporte buscarParaOperador(String id, String operadorId) {
        Reporte reporte = buscar(id);
        if (!reporte.estaAsignadoA(operadorId)) {
            throw new RecursoNoEncontradoException("Reporte asignado", id);
        }
        return reporte;
    }

    /** Servicio "Consultar y actualizar estado del reporte": guarda el cambio y notifica al ciudadano. */
    public Reporte actualizarEstado(Reporte reporte) {
        Reporte guardado = guardarConControlDeConcurrencia(reporte);
        notificacionService.notificarCambioEstado(guardado);
        return guardado;
    }

    // ============================================================ métodos privados de apoyo

    /** Solo se asignan reportes a operadores disponibles (HU-07); la regla la aplica el propio operador. */
    private void validarDisponibilidad(Operador operador) {
        operador.verificarDisponible();
    }

    private Reporte buscar(String id) {
        return reporteRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reporte", id));
    }

    private Ciudadano buscarCiudadano(String ciudadanoId) {
        return ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudadano", ciudadanoId));
    }

    private Operador buscarOperador(String operadorId) {
        return operadorRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador", operadorId));
    }

    /** Si otro usuario guardó el mismo reporte un instante antes, MongoDB rechaza este guardado (@Version). */
    private Reporte guardarConControlDeConcurrencia(Reporte reporte) {
        try {
            return reporteRepository.save(reporte);
        } catch (OptimisticLockingFailureException e) {
            throw new ReglaNegocioException("Otro usuario modificó el reporte al mismo tiempo; recargue e intente de nuevo");
        }
    }

    /**
     * Filtros opcionales de la bandeja y del historial. Un filtro en null significa "no filtrar por eso".
     * <b>Experto en información:</b> el propio filtro decide si un reporte lo cumple.
     */
    public record FiltroReportes(EstadoReporte estado, TipoResiduo tipoResiduo, Prioridad prioridad, String sector) {
        boolean cumple(Reporte r) {
            return (estado == null || r.estadoVisible() == estado)
                    && (tipoResiduo == null || r.getTipoResiduo() == tipoResiduo)
                    && (prioridad == null || r.getPrioridad() == prioridad)
                    && (sector == null || sector.equalsIgnoreCase(r.getSector()));
        }
    }
}
