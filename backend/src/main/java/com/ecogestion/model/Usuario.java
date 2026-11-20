package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Datos y comportamiento comunes a todas las cuentas del sistema: ciudadano, operador y administrador.
 * Guarda las credenciales (la contraseña nunca se guarda en texto plano, solo su hash BCrypt) y
 * controla el bloqueo temporal tras varios intentos fallidos de inicio de sesión (HU-01).
 *
 * <p><b>Encapsulamiento:</b> la clase no tiene setters. Los datos solo cambian con métodos que validan
 * (por ejemplo {@link #cambiarContrasena} exige la contraseña actual). Los getters que existen son de
 * solo lectura y cada uno tiene un uso concreto, indicado en su comentario. Spring Data lee y escribe los
 * atributos directamente (por reflexión), así que no necesita setters para guardar en MongoDB.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> cada subclase responde {@link #getRol()} a su manera. Spring Security y los
 *       servicios trabajan con "un Usuario" sin preguntar con if/else de qué tipo es.</li>
 *   <li><b>Experto en información:</b> el usuario conoce su contraseña cifrada, sus intentos fallidos y su
 *       fecha de bloqueo, por eso es él quien verifica la contraseña, decide si está bloqueado y cuenta
 *       los intentos. Ningún servicio necesita leer esos datos para decidir.</li>
 * </ul>
 */
public abstract class Usuario {

    @Id
    protected String id;
    protected String nombre;
    @Indexed(unique = true)
    protected String documento;
    @Indexed(unique = true)
    protected String correo;
    protected String contrasenaHash;
    protected int intentosFallidos;
    protected LocalDateTime bloqueadoHasta;

    protected Usuario() {
    }

    protected Usuario(String nombre, String documento, String correo, String contrasenaHash) {
        this.documento = obligatorio(documento, "El documento es obligatorio");
        this.contrasenaHash = obligatorio(contrasenaHash, "La contraseña es obligatoria");
        actualizarNombreYCorreo(nombre, correo);
    }

    /** Rol con el que la cuenta actúa en el sistema. Cada subclase devuelve el suyo. */
    public abstract Rol getRol();

    // ============================================================ Contraseña (HU-01, HU-02)

    /**
     * Cambia la contraseña solo si la actual es correcta. El {@link PasswordEncoder} (BCrypt) se recibe
     * como parámetro: el usuario no sabe qué algoritmo se usa, solo pide comparar y cifrar.
     */
    public void cambiarContrasena(String actual, String nueva, PasswordEncoder cifrador) {
        if (!cifrador.matches(actual, contrasenaHash)) {
            throw new DatosInvalidosException("La contraseña actual no es correcta");
        }
        this.contrasenaHash = cifrador.encode(nueva);
    }

    // ============================================================ Bloqueo por intentos fallidos (HU-01)

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Suma un intento fallido; al llegar al máximo bloquea la cuenta durante los minutos indicados. */
    public void registrarIntentoFallido(LocalDateTime ahora, int intentosMaximos, int minutosBloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= intentosMaximos) {
            bloqueadoHasta = ahora.plusMinutes(minutosBloqueo);
            intentosFallidos = 0;
        }
    }

    /** Indica si hay intentos fallidos o un bloqueo que limpiar después de un ingreso correcto. */
    public boolean tieneIntentosFallidos() {
        return intentosFallidos > 0 || bloqueadoHasta != null;
    }

    /** Se llama cuando el inicio de sesión es correcto. */
    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    // ============================================================ Apoyo para las subclases

    /** Solo las subclases pueden cambiar nombre y correo (el ciudadano desde "Mi perfil"). */
    protected final void actualizarNombreYCorreo(String nuevoNombre, String nuevoCorreo) {
        this.nombre = obligatorio(nuevoNombre, "El nombre es obligatorio");
        // El correo se guarda en minúsculas para que el inicio de sesión no distinga mayúsculas
        this.correo = obligatorio(nuevoCorreo, "El correo es obligatorio").toLowerCase();
    }

    private static String obligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException(mensaje);
        }
        return valor.trim();
    }

    // ============================================================ Consultas de solo lectura

    /** Identifica al usuario en la sesión, en las referencias de los reportes y en las respuestas de la API. */
    public String getId() { return id; }

    /** Se muestra en la interfaz, en las notificaciones y en la asignación de reportes. */
    public String getNombre() { return nombre; }

    /** Se muestra en "Mi perfil" (UsuarioResponse). */
    public String getDocumento() { return documento; }

    /** Es el nombre de usuario para iniciar sesión y el destino de las notificaciones. */
    public String getCorreo() { return correo; }

    /**
     * Hash BCrypt de la contraseña. Solo lo usa {@code CuentaUsuario}, porque Spring Security necesita
     * compararlo al iniciar sesión. Nunca se envía en la API (UsuarioResponse no lo incluye).
     */
    public String getContrasenaHash() { return contrasenaHash; }
}
