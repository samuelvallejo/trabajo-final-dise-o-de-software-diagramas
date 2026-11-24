package com.ecogestion.config;

import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.model.ContenidoEducativo;
import com.ecogestion.model.Coordenada;
import com.ecogestion.model.HorarioRecoleccion;
import com.ecogestion.model.MaterialAceptado;
import com.ecogestion.model.PuntoRecoleccion;
import com.ecogestion.model.RutaRecoleccion;
import com.ecogestion.model.Sector;
import com.ecogestion.repository.ContenidoEducativoRepository;
import com.ecogestion.repository.PuntoRecoleccionRepository;
import com.ecogestion.repository.RutaRecoleccionRepository;
import com.ecogestion.repository.SectorRepository;
import com.ecogestion.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Se ejecuta una vez al arrancar la aplicación ({@link ApplicationRunner}):
 * <ol>
 *   <li>crea el administrador inicial con las variables ADMIN_CORREO y ADMIN_CONTRASENA;</li>
 *   <li>si la base está vacía y DATOS_DEMO=true, carga datos de ejemplo de Pasto: 5 sectores, 3 rutas,
 *       3 puntos de recolección y 2 contenidos educativos.</li>
 * </ol>
 * Los límites de los sectores son rectángulos aproximados y solo sirven para la demostración.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> clase técnica de arranque; no forma parte del dominio.</li>
 * </ul>
 */
@Component
public class DatosIniciales implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosIniciales.class);

    private final EcoGestionProperties propiedades;
    private final UsuarioService usuarioService;
    private final SectorRepository sectorRepository;
    private final RutaRecoleccionRepository rutaRepository;
    private final PuntoRecoleccionRepository puntoRepository;
    private final ContenidoEducativoRepository contenidoRepository;

    public DatosIniciales(EcoGestionProperties propiedades, UsuarioService usuarioService, SectorRepository sectorRepository,
                          RutaRecoleccionRepository rutaRepository, PuntoRecoleccionRepository puntoRepository,
                          ContenidoEducativoRepository contenidoRepository) {
        this.propiedades = propiedades;
        this.usuarioService = usuarioService;
        this.sectorRepository = sectorRepository;
        this.rutaRepository = rutaRepository;
        this.puntoRepository = puntoRepository;
        this.contenidoRepository = contenidoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (propiedades.admin() != null && propiedades.admin().configurado()
                && usuarioService.crearAdministradorInicial(propiedades.admin().correo(), propiedades.admin().contrasena())) {
            LOG.info("Administrador inicial creado: {}", propiedades.admin().correo());
        }
        if (propiedades.datosDemo() && sectorRepository.count() == 0) {
            cargarDatosDemo();
            LOG.info("Datos de demostración de Pasto cargados");
        }
    }

    private void cargarDatosDemo() {
        // Cuadrícula aproximada alrededor del centro de Pasto (1.2136, -77.2811)
        sectorRepository.saveAll(List.of(
                sector("Centro", "Comuna 1", List.of("San Andrés", "Santiago", "Las Cuadras"), 1.205, 1.222, -77.290, -77.272),
                sector("Norte", "Comuna 9", List.of("Torobajo", "Morasurco", "Pandiaco"), 1.222, 1.260, -77.300, -77.255),
                sector("Sur", "Comuna 5", List.of("Chapal", "El Ejido", "Potrerillo"), 1.170, 1.205, -77.300, -77.255),
                sector("Oriente", "Comuna 10", List.of("La Carolina", "Las Lunas", "Santa Mónica"), 1.205, 1.222, -77.272, -77.240),
                sector("Occidente", "Comuna 6", List.of("Tamasagra", "Anganoy", "Mijitayo"), 1.205, 1.222, -77.320, -77.290)));

        rutaRepository.saveAll(List.of(
                new RutaRecoleccion("Ruta 1 – Centro", List.of("Centro"),
                        List.of(horario(DayOfWeek.MONDAY, 19, 23), horario(DayOfWeek.WEDNESDAY, 19, 23), horario(DayOfWeek.FRIDAY, 19, 23)),
                        List.of(new Coordenada(1.2136, -77.2811), new Coordenada(1.2150, -77.2780))),
                new RutaRecoleccion("Ruta 2 – Norte y Oriente", List.of("Norte", "Oriente"),
                        List.of(horario(DayOfWeek.TUESDAY, 6, 12), horario(DayOfWeek.THURSDAY, 6, 12), horario(DayOfWeek.SATURDAY, 6, 10)),
                        List.of()),
                new RutaRecoleccion("Ruta 3 – Sur y Occidente", List.of("Sur", "Occidente"),
                        List.of(horario(DayOfWeek.MONDAY, 6, 12), horario(DayOfWeek.THURSDAY, 6, 12)),
                        List.of())));

        puntoRepository.saveAll(List.of(
                new PuntoRecoleccion("Punto limpio Centro", "Carrera 24 con Calle 18", "Centro", 1.2140, -77.2805,
                        EnumSet.of(MaterialAceptado.RECICLABLES, MaterialAceptado.PILAS), "Lunes a sábado 8:00–17:00"),
                new PuntoRecoleccion("Ecopunto Unicentro", "Avenida Panamericana, Unicentro", "Norte", 1.2310, -77.2850,
                        EnumSet.of(MaterialAceptado.ELECTRONICOS, MaterialAceptado.PILAS), "Todos los días 10:00–20:00"),
                new PuntoRecoleccion("Centro de acopio Sur", "Calle 10 sur, Chapal", "Sur", 1.1950, -77.2760,
                        EnumSet.of(MaterialAceptado.RESIDUOS_ESPECIALES, MaterialAceptado.RECICLABLES), "Martes a viernes 7:00–15:00")));

        ContenidoEducativo separacion = new ContenidoEducativo("Cómo separar los residuos en casa",
                CategoriaContenido.SEPARACION_RESIDUOS,
                "Use la bolsa blanca para aprovechables limpios y secos, la negra para no aprovechables y la verde para orgánicos.",
                null, null);
        separacion.publicar();
        ContenidoEducativo pilas = new ContenidoEducativo("¿Qué hacer con las pilas usadas?", CategoriaContenido.BUENAS_PRACTICAS,
                "Nunca las bote con la basura común: llévelas a un punto de recolección autorizado de pilas.", null, null);
        pilas.publicar();
        contenidoRepository.saveAll(List.of(separacion, pilas));
    }

    private static Sector sector(String nombre, String comuna, List<String> barrios,
                                 double latMin, double latMax, double lonMin, double lonMax) {
        GeoJsonPolygon limite = new GeoJsonPolygon(new Point(lonMin, latMin), new Point(lonMax, latMin),
                new Point(lonMax, latMax), new Point(lonMin, latMax), new Point(lonMin, latMin));
        return new Sector(nombre, comuna, barrios, limite);
    }

    private static HorarioRecoleccion horario(DayOfWeek dia, int desde, int hasta) {
        return new HorarioRecoleccion(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }
}
