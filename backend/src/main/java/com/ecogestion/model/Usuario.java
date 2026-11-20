package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Datos y comportamiento comunes de las cuentas del sistema (ciudadano, operador y administrador):
 * credenciales cifradas con BCrypt y bloqueo temporal tras varios intentos fallidos (HU-01).
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
        this.correo = correo == null ? null : correo.trim().toLowerCase();
        this.contrasenaHash = contrasenaHash;
    }

    /** Rol con el que el usuario actúa en el sistema. */
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

    public void reiniciarIntentos() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo == null ? null : correo.trim().toLowerCase(); }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
}
