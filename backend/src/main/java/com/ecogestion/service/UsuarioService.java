package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.UsuarioDto.OperadorRequest;
import com.ecogestion.dto.UsuarioDto.PerfilRequest;
import com.ecogestion.dto.UsuarioDto.RegistroRequest;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.excepcion.ReglaNegocioException;
import com.ecogestion.model.Administrador;
import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Rol;
import com.ecogestion.model.Usuario;
import com.ecogestion.repository.AdministradorRepository;
import com.ecogestion.repository.CiudadanoRepository;
import com.ecogestion.repository.OperadorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lógica del componente "Gestión de usuarios": registro e inicio de sesión (HU-01), perfil (HU-02) y
 * cuentas de operadores. También provee a otros componentes los servicios de la vista funcional
 * "Validar ciudadano", "Obtener correo del ciudadano" y "Validar rol de administrador".
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Creador:</b> crea {@link Ciudadano}, {@link Operador} y {@link Administrador} porque recibe los
 *       datos de registro y es responsable de cifrar la contraseña antes de crearlos.</li>
 *   <li><b>Alta cohesión:</b> todo lo relacionado con cuentas está aquí y en ningún otro servicio.</li>
 *   <li><b>Polimorfismo:</b> trabaja con {@link Usuario} sin importar el tipo concreto; solo
 *       {@link #guardar} necesita saber en qué colección va cada tipo.</li>
 *   <li><b>Bajo acoplamiento:</b> usa la interfaz {@link PasswordEncoder}; no sabe que por debajo es BCrypt.</li>
 * </ul>
 */
@Service
public class UsuarioService {

    private final CiudadanoRepository ciudadanoRepository;
    private final OperadorRepository operadorRepository;
    private final AdministradorRepository administradorRepository;
    private final HorarioRutaService horarioRutaService;
    private final PasswordEncoder passwordEncoder;
    private final EcoGestionProperties.Seguridad seguridad;

    public UsuarioService(CiudadanoRepository ciudadanoRepository, OperadorRepository operadorRepository,
                          AdministradorRepository administradorRepository, HorarioRutaService horarioRutaService,
                          PasswordEncoder passwordEncoder, EcoGestionProperties propiedades) {
        this.ciudadanoRepository = ciudadanoRepository;
        this.operadorRepository = operadorRepository;
        this.administradorRepository = administradorRepository;
        this.horarioRutaService = horarioRutaService;
        this.passwordEncoder = passwordEncoder;
        this.seguridad = propiedades.seguridad();
    }

    // ============================================================ HU-01: registro e inicio de sesión

    /** Crea la cuenta de un ciudadano. El correo y el documento no pueden repetirse. */
    public Ciudadano registrarCiudadano(RegistroRequest datos) {
        String correo = normalizar(datos.correo());
        validarCorreoLibre(correo);
        if (ciudadanoRepository.existsByDocumento(datos.documento())) {
            throw new ReglaNegocioException("El documento ya está registrado");
        }
        Ciudadano ciudadano = new Ciudadano(datos.nombre(), datos.documento(), correo,
                passwordEncoder.encode(datos.contrasena()), validarSector(datos.sector()));
        return ciudadanoRepository.save(ciudadano);
    }

    /** Busca una cuenta por correo en las tres colecciones (el correo es único en todo el sistema). */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        String c = normalizar(correo);
        return ciudadanoRepository.findByCorreo(c).<Usuario>map(u -> u)
                .or(() -> operadorRepository.findByCorreo(c).map(u -> u))
                .or(() -> administradorRepository.findByCorreo(c).map(u -> u));
    }

    /** Cuenta un intento fallido; al quinto la cuenta queda bloqueada 15 minutos (HU-01, criterio 5). */
    public void registrarIntentoFallido(String correo) {
        buscarPorCorreo(correo).ifPresent(usuario -> {
            usuario.registrarIntentoFallido(LocalDateTime.now(), seguridad.intentosMaximos(), seguridad.minutosBloqueo());
            guardar(usuario);
        });
    }

    /** Al entrar correctamente se borran los intentos fallidos acumulados. */
    public void registrarIngresoExitoso(String correo) {
        buscarPorCorreo(correo)
                .filter(Usuario::tieneIntentosFallidos)
                .ifPresent(usuario -> {
                    usuario.reiniciarIntentos();
                    guardar(usuario);
                });
    }

    // ============================================================ HU-02: perfil del ciudadano

    /** Servicio "Validar ciudadano": devuelve el ciudadano o falla si no existe. */
    public Ciudadano validarCiudadano(String ciudadanoId) {
        return ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudadano", ciudadanoId));
    }

    public Ciudadano actualizarPerfil(String ciudadanoId, PerfilRequest datos) {
        Ciudadano ciudadano = validarCiudadano(ciudadanoId);
        String correo = normalizar(datos.correo());
        if (!correo.equals(ciudadano.getCorreo())) {
            validarCorreoLibre(correo);
        }
        ciudadano.actualizarPerfil(datos.nombre(), correo, validarSector(datos.sector()));
        return ciudadanoRepository.save(ciudadano);
    }

    /** Cambia la contraseña; el propio usuario verifica que la actual sea correcta (Experto en información). */
    public void cambiarContrasena(String correoUsuario, String actual, String nueva) {
        Usuario usuario = buscarPorCorreo(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoUsuario));
        usuario.cambiarContrasena(actual, nueva, passwordEncoder);
        guardar(usuario);
    }

    /** Servicio "Obtener correo del ciudadano" que usa Notificaciones (HU-11). */
    public String obtenerCorreoDelCiudadano(String ciudadanoId) {
        return validarCiudadano(ciudadanoId).getCorreo();
    }

    // ============================================================ operadores (los crea el administrador)

    public Operador crearOperador(OperadorRequest datos) {
        String correo = normalizar(datos.correo());
        validarCorreoLibre(correo);
        if (operadorRepository.existsByDocumento(datos.documento())) {
            throw new ReglaNegocioException("El documento ya está registrado");
        }
        return operadorRepository.save(new Operador(datos.nombre().trim(), datos.documento().trim(), correo,
                passwordEncoder.encode(datos.contrasena())));
    }

    /** Servicio "Consultar operadores" que usa Asignación y atención (HU-07). */
    public List<Operador> consultarOperadores(boolean soloDisponibles) {
        return soloDisponibles ? operadorRepository.findByDisponibleTrueOrderByNombreAsc() : operadorRepository.findAll();
    }

    /** Marca al operador como disponible u ocupado; el propio operador cambia su estado. */
    public Operador cambiarDisponibilidad(String operadorId, boolean disponible) {
        Operador operador = operadorRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador", operadorId));
        if (disponible) {
            operador.marcarDisponible();
        } else {
            operador.marcarOcupado();
        }
        return operadorRepository.save(operador);
    }

    // ============================================================ administración

    /** Servicio "Validar rol de administrador" que usa Contenido educativo (HU-14). */
    public void validarRolAdministrador(Rol rol) {
        if (rol != Rol.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo un administrador puede realizar esta acción");
        }
    }

    /** Crea el primer administrador al arrancar (variables ADMIN_CORREO y ADMIN_CONTRASENA). */
    public boolean crearAdministradorInicial(String correo, String contrasena) {
        String c = normalizar(correo);
        if (administradorRepository.existsByCorreo(c)) {
            return false;
        }
        administradorRepository.save(new Administrador("Administrador EcoGestión", "ADMIN-" + c, c,
                passwordEncoder.encode(contrasena)));
        return true;
    }

    // ============================================================ métodos privados de apoyo

    private void validarCorreoLibre(String correo) {
        boolean enUso = ciudadanoRepository.existsByCorreo(correo) || operadorRepository.existsByCorreo(correo)
                || administradorRepository.existsByCorreo(correo);
        if (enUso) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
    }

    /** El sector es opcional; si viene, debe existir (se guarda con el nombre oficial). */
    private String validarSector(String sector) {
        if (sector == null || sector.isBlank()) {
            return null;
        }
        return horarioRutaService.consultarSector(sector).getNombre();
    }

    /** Cada tipo de usuario vive en su propia colección, así que se guarda con su repositorio. */
    private void guardar(Usuario usuario) {
        switch (usuario) {
            case Ciudadano c -> ciudadanoRepository.save(c);
            case Operador o -> operadorRepository.save(o);
            case Administrador a -> administradorRepository.save(a);
            default -> throw new IllegalStateException("Tipo de usuario no soportado");
        }
    }

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
