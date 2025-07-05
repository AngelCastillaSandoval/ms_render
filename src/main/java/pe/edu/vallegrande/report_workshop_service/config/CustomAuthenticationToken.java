package pe.edu.vallegrande.report_workshop_service.config;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * CustomAuthenticationToken extiende AbstractAuthenticationToken
 * y representa al usuario autenticado a través de un JWT (token de Firebase en este caso).
 * Se usa para personalizar la información de autenticación y roles.
 */
@Getter
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    // Token JWT original con todos los claims (incluye sub, email, role, etc.)
    private final Jwt jwt;

    /**
     * 🔧 Constructor del token personalizado.
     *
     * @param jwt           El token JWT ya validado
     * @param authorities   Los roles o permisos extraídos del JWT
     */
    public CustomAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        setAuthenticated(true);
    }

    /**
     * Devuelve el token como credenciales (aunque no se usa directamente)
     */
    @Override
    public Object getCredentials() {
        return jwt;
    }

    /**
     * Devuelve el sujeto (usuario) como principal
     */
    @Override
    public Object getPrincipal() {
        return jwt.getSubject();
    }

    /**
     * Devuelve el nombre del usuario (por defecto usa el "sub" del JWT)
     */
    @Override
    public String getName() {
        return jwt.getSubject();
    }
}
