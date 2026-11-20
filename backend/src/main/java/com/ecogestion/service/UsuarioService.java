package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.UsuarioDto.OperadorRequest;
import com.ecogestion.dto.UsuarioDto.PerfilRequest;
import com.ecogestion.dto.UsuarioDto.RegistroRequest;
import com.ecogestion.excepcion.DatosInvalidosException;
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
 * Componente "Gestión de usuarios" (vista funcional). Provee: Validar ciudadano, Consultar operadores,
 * Obtener correo del ciudadano y Validar rol de administrador. Historias HU-01, HU-02 y parte de HU-07.
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

    // ------------------------------------------------------------------ HU-01 registro

    public Ciudadano registrarCiudadano(RegistroRequest datos) {
        String correo = normalizar(datos.correo());
        if (correoEnUso(correo)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (ciudadanoRepository.existsByDocumento(datos.documento())) {
            throw new ReglaNegocioException("El documento ya está registrado");
        }
        Ciudadano ciudadano = new Ciudadano(datos.nombre().trim(), datos.documento().trim(), correo,
                passwordEncoder.encode(datos.contrasena()));
        ciudadano.setSector(validarSector(datos.sector()));
        return ciudadanoRepository.save(ciudadano);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        String c = normalizar(correo);
        return ciudadanoRepository.findByCorreo(c).<Usuario>map(u -> u)
                .or(() -> operadorRepository.findByCorreo(c).map(u -> u))
                .or(() -> administradorRepository.findByCorreo(c).map(u -> u));
    }

    /** Cuenta un intento fallido y bloquea 15 minutos al llegar a 5 (HU-01, criterio 5). */
    public void registrarIntentoFallido(String correo) {
        buscarPorCorreo(correo).ifPresent(usuario -> {
            usuario.registrarIntentoFallido(LocalDateTime.now(), seguridad.intentosMaximos(), seguridad.minutosBloqueo());
            guardar(usuario);
        });
    }

    public void registrarIngresoExitoso(String correo) {
        buscarPorCorreo(correo).filter(u -> u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null)
                .ifPresent(usuario -> {
                    usuario.reiniciarIntentos();
                    guardar(usuario);
                });
    }

    // ------------------------------------------------------------------ HU-02 perfil

    public Ciudadano validarCiudadano(String ciudadanoId) {
        return ciudadanoRepository.findById(ciudadanoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ciudadano", ciudadanoId));
    }

    public Ciudadano actualizarPerfil(String ciudadanoId, PerfilRequest datos) {
        Ciudadano ciudadano = validarCiudadano(ciudadanoId);
        String correo = normalizar(datos.correo());
        if (!correo.equals(ciudadano.getCorreo()) && correoEnUso(correo)) {
            throw new ReglaNegocioException("El correo ya está en uso por otra cuenta");
        }
        ciudadano.setNombre(datos.nombre().trim());
        ciudadano.setCorreo(correo);
        ciudadano.setSector(validarSector(datos.sector()));
        return ciudadanoRepository.save(ciudadano);
    }

    public void cambiarContrasena(String correoUsuario, String actual, String nueva) {
        Usuario usuario = buscarPorCorreo(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoUsuario));
        if (!passwordEncoder.matches(actual, usuario.getContrasenaHash())) {
            throw new DatosInvalidosException("La contraseña actual no es correcta");
        }
        usuario.setContrasenaHash(passwordEncoder.encode(nueva));
        guardar(usuario);
    }

    /** "Obtener correo del ciudadano" (lo consume Notificaciones, HU-11). */
    public String obtenerCorreoDelCiudadano(String ciudadanoId) {
        return validarCiudadano(ciudadanoId).getCorreo();
    }

    // ------------------------------------------------------------------ operadores (HU-01 nota, HU-07)

    public Operador crearOperador(OperadorRequest datos) {
        String correo = normalizar(datos.correo());
        if (correoEnUso(correo)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (operadorRepository.existsByDocumento(datos.documento())) {
            throw new ReglaNegocioException("El documento ya está registrado");
        }
        return operadorRepository.save(new Operador(datos.nombre().trim(), datos.documento().trim(), correo,
                passwordEncoder.encode(datos.contrasena())));
    }

    /** "Consultar operadores" (lo consume Asignación y atención). */
    public List<Operador> consultarOperadores(boolean soloDisponibles) {
        return soloDisponibles ? operadorRepository.findByDisponibleTrueOrderByNombreAsc() : operadorRepository.findAll();
    }

    public Operador buscarOperador(String operadorId) {
        return operadorRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Operador", operadorId));
    }

    public Operador cambiarDisponibilidad(String operadorId, boolean disponible) {
        Operador operador = buscarOperador(operadorId);
        if (disponible) {
            operador.marcarDisponible();
        } else {
            operador.marcarOcupado();
        }
        return operadorRepository.save(operador);
    }

    // ------------------------------------------------------------------ administración

    /** "Validar rol de administrador" (lo consume Contenido educativo, HU-14). */
    public void validarRolAdministrador(Rol rol) {
        if (rol != Rol.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo un administrador puede realizar esta acción");
        }
    }

    /** Crea el administrador inicial si aún no existe (variables ADMIN_CORREO y ADMIN_CONTRASENA). */
    public boolean crearAdministradorInicial(String correo, String contrasena) {
        String c = normalizar(correo);
        if (administradorRepository.existsByCorreo(c)) {
            return false;
        }
        administradorRepository.save(new Administrador("Administrador EcoGestión", "ADMIN-" + c, c,
                passwordEncoder.encode(contrasena)));
        return true;
    }

    // ------------------------------------------------------------------ apoyo

    private boolean correoEnUso(String correo) {
        return ciudadanoRepository.existsByCorreo(correo) || operadorRepository.existsByCorreo(correo)
                || administradorRepository.existsByCorreo(correo);
    }

    private String validarSector(String sector) {
        if (sector == null || sector.isBlank()) {
            return null;
        }
        return horarioRutaService.consultarSector(sector).getNombre();
    }

    private Usuario guardar(Usuario usuario) {
        return switch (usuario) {
            case Ciudadano c -> ciudadanoRepository.save(c);
            case Operador o -> operadorRepository.save(o);
            case Administrador a -> administradorRepository.save(a);
            default -> throw new IllegalStateException("Tipo de usuario no soportado");
        };
    }

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
