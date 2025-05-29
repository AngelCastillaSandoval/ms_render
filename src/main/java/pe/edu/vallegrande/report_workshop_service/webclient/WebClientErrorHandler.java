package pe.edu.vallegrande.report_workshop_service.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class WebClientErrorHandler {

    public <T> Mono<T> handleError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            log.error("Error WebClient - Status: {}, Body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
        } else {
            log.error("Error inesperado WebClient", throwable);
        }
        return Mono.empty();
    }
}