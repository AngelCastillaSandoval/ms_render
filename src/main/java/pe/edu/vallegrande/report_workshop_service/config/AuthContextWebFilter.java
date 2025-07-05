package pe.edu.vallegrande.report_workshop_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class AuthContextWebFilter {

    /**
     * WebFilter que intercepta la cabecera Authorization y propaga el token JWT
     * dentro del contexto Reactor, permitiendo su acceso en flujos reactivos posteriores.
     */
    @Bean
    public WebFilter jwtTokenPropagationFilter() {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // Propaga el token en el contexto Reactor (clave: "Authorization")
                return chain.filter(exchange).contextWrite(ctx -> ctx.put("Authorization", token));
            }
            return chain.filter(exchange);
        };
    }
}

