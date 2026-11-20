package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Datos y comportamiento comunes a todas las cuentas del sistema: ciudadano, operador y administrador.
 * Guarda las credenciales (la contraseña nunca se guarda en texto plano, solo su hash BCrypt) y
 * controla el bloqueo temporal tras varios intentos fallidos de inicio de sesión (HU-01).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> cada subclase responde {@link #getRol()} a su manera. Spring Security y los
 *       servicios trabajan con "un Usuario" sin preguntar con if/else de qué tipo es.</li>
 *   <li><b>Experto en información:</b> el usuario conoce sus intentos fallidos y su fecha de bloqueo,
 *       por eso él mismo decide si está bloqueado ({@link #estaBloqueado}).</li>
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
        this.nombre = nombre;
        this.documento = documento;
        setCorreo(correo);
        this.contrasenaHash = contrasenaHash;
    }

    /** Rol con el que la cuenta actúa en el sistema. Cada subclase devuelve el suyo. */
    public abstract Rol getRol();

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

    /** Se llama cuando el inicio de sesión es correcto. */
    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDocumento() { return documento; }
    public String getCorreo() { return correo; }
    /** El correo se guarda siempre en minúsculas para que el inicio de sesión no distinga mayúsculas. */
    public void setCorreo(String correo) { this.correo = correo == null ? null : correo.trim().toLowerCase(); }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
}
