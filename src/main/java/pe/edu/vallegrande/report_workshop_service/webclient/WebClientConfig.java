package pe.edu.vallegrande.report_workshop_service.webclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuración del WebClient para comunicarse con el microservicio core (core-service).
 * Agrega automáticamente el token JWT del contexto si está presente.
 */
@Configuration
public class WebClientConfig {

    @Value("${core-service.url}")
    private String baseUrl;

    /**
     * Configura el WebClient con base URL y filtro JWT.
     */
    @Bean
    public WebClient coreServiceWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(authHeaderFilter()) // Añade token JWT si existe en el contexto
                .build();
    }

    /**
     * Filtro que agrega Authorization si está presente.
     */
    private ExchangeFilterFunction authHeaderFilter() {
        return (request, next) -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey("Authorization")) {
                String token = ctx.get("Authorization");
                return next.exchange(
                        ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(token))
                                .build()
                );
            }
            return next.exchange(request);
        });
    }
}
