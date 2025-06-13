package pe.edu.vallegrande.report_workshop_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collection;
import java.util.List;

/**
 * Configura la seguridad del microservicio:
 * - Permite acceso sin token a Swagger
 * - Requiere JWT con roles USER/ADMIN para acceder a las rutas protegidas
 * - Configura CORS para permitir acceso desde el frontend
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Permitir preflight CORS sin token
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger (libre)
                        .pathMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // GET: accesible por USER y ADMIN
                        .pathMatchers(HttpMethod.GET, "/api/reports-workshop/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/workshop-cache/**").hasAnyRole("USER", "ADMIN")

                        // POST, PUT, DELETE: solo ADMIN
                        .pathMatchers(HttpMethod.POST, "/api/reports-workshop/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/reports-workshop/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/reports-workshop/**").hasRole("ADMIN")

                        // Todo lo demás requiere estar autenticado
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(this::convertJwt)
                        )
                )
                .cors(cors -> cors
                        .configurationSource(exchange -> {
                            var config = new org.springframework.web.cors.CorsConfiguration();
                            config.setAllowCredentials(true);
                            config.addAllowedOrigin("http://localhost:4200");
                            config.addAllowedHeader("*");
                            config.addAllowedMethod("*");
                            return config;
                        })
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    private Mono<CustomAuthenticationToken> convertJwt(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        Collection<GrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                : List.of();
        return Mono.just(new CustomAuthenticationToken(jwt, authorities));
    }
}
