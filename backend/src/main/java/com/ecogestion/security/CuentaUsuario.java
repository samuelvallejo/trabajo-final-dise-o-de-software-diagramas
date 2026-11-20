package com.ecogestion.security;

import com.ecogestion.model.Rol;
import com.ecogestion.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado tal como lo ve Spring Security durante la sesión: id, correo, hash de la contraseña,
 * rol y si la cuenta está bloqueada. Se guarda en la sesión HTTP; los controladores lo reciben con
 * {@code @AuthenticationPrincipal CuentaUsuario cuenta}.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Indirección (adaptador):</b> adapta un {@code Usuario} del dominio a la interfaz {@code UserDetails}
 *       que exige Spring Security, sin que las clases del modelo dependan de Spring Security.</li>
 *   <li><b>Bajo acoplamiento:</b> los controladores solo necesitan el id y el rol, no el documento completo.</li>
 * </ul>
 */
public class CuentaUsuario implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String correo;
    private final String contrasenaHash;
    private final Rol rol;
    private final boolean bloqueada;

    public CuentaUsuario(Usuario usuario, LocalDateTime ahora) {
        this.id = usuario.getId();
        this.correo = usuario.getCorreo();
        this.contrasenaHash = usuario.getContrasenaHash();
        this.rol = usuario.getRol();
        this.bloqueada = usuario.estaBloqueado(ahora);
    }

    public String getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.autoridad()));
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueada;
    }
}
