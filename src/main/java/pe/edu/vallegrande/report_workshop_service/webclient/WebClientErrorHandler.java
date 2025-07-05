package pe.edu.vallegrande.report_workshop_service.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Manejador global de errores para llamadas WebClient.
 * Registra errores HTTP o excepciones generales y devuelve Mono.empty().
 */
@Slf4j
@Component
public class WebClientErrorHandler {

    /**
     * Procesa errores lanzados por WebClient.
     */
    public <T> Mono<T> handleError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            log.error("WebClient error - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        } else {
            log.error("Error inesperado en WebClient", throwable);
        }
        return Mono.empty();
    }
}
