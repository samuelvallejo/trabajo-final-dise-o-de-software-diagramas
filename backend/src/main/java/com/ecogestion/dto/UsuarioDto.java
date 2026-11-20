package com.ecogestion.dto;

import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Rol;
import com.ecogestion.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Objetos de transferencia (DTO) de cuentas: registro, inicio de sesión, perfil y operadores (HU-01, HU-02, HU-07).
 *
 * <p><b>Cómo funciona:</b> cada {@code record} es un objeto inmutable con los datos que entran
 * ({@code ...Request}, validados con anotaciones como {@code @NotBlank}) o que salen ({@code ...Response})
 * de la API en formato JSON. Los métodos {@code de(...)} convierten un objeto del modelo en su respuesta.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> la API no expone las clases del modelo; si cambia un documento de
 *       MongoDB, el JSON que usa el frontend no cambia. Tampoco se envían datos sensibles
 *       (por ejemplo el hash de la contraseña).</li>
 *   <li><b>Creador:</b> cada respuesta sabe construirse a partir de su objeto del modelo ({@code de}).</li>
 * </ul>
 */
public final class UsuarioDto {

    /** Mínimo 8 caracteres con letras y números (HU-01, criterio 3). */
    public static final String PATRON_CONTRASENA = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String MENSAJE_CONTRASENA = "La contraseña debe tener al menos 8 caracteres, con letras y números";

    private UsuarioDto() {
    }

    public record RegistroRequest(
            @NotBlank String nombre,
            @NotBlank String documento,
            @NotBlank @Email String correo,
            @NotBlank @Pattern(regexp = PATRON_CONTRASENA, message = MENSAJE_CONTRASENA) String contrasena,
            String sector) { }

    public record LoginRequest(@NotBlank @Email String correo, @NotBlank String contrasena) { }

    public record PerfilRequest(@NotBlank String nombre, @NotBlank @Email String correo, String sector) { }

    public record CambioContrasenaRequest(
            @NotBlank String actual,
            @NotBlank @Pattern(regexp = PATRON_CONTRASENA, message = MENSAJE_CONTRASENA) String nueva) { }

    public record OperadorRequest(
            @NotBlank String nombre,
            @NotBlank String documento,
            @NotBlank @Email String correo,
            @NotBlank @Pattern(regexp = PATRON_CONTRASENA, message = MENSAJE_CONTRASENA) String contrasena) { }

    public record DisponibilidadRequest(boolean disponible) { }

    public record UsuarioResponse(String id, String nombre, String documento, String correo, Rol rol, String sector) {
        public static UsuarioResponse de(Usuario usuario) {
            String sector = usuario instanceof Ciudadano c ? c.getSector() : null;
            return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getDocumento(),
                    usuario.getCorreo(), usuario.getRol(), sector);
        }
    }

    /** Operador con su carga de trabajo para elegir a quién asignar (HU-07, criterio 4). */
    public record OperadorResponse(String id, String nombre, String correo, boolean disponible, long reportesSinResolver) {
        public static OperadorResponse de(Operador o, long reportesSinResolver) {
            return new OperadorResponse(o.getId(), o.getNombre(), o.getCorreo(), o.isDisponible(), reportesSinResolver);
        }
    }
}
