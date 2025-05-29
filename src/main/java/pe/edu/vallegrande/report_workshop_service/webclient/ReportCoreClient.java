package pe.edu.vallegrande.report_workshop_service.webclient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.edu.vallegrande.report_workshop_service.dto.ReportDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ReportCoreClient {

    private final WebClient coreServiceWebClient;

    private static final String BASE_PATH = "/api/reports";

    public Flux<ReportDto> findAll() {
        return coreServiceWebClient.get()
                .uri(BASE_PATH)
                .retrieve()
                .bodyToFlux(ReportDto.class);
    }

    public Mono<ReportDto> findById(Integer id) {
        return coreServiceWebClient.get()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .bodyToMono(ReportDto.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    public Mono<ReportDto> create(ReportDto dto) {
        return coreServiceWebClient.post()
                .uri(BASE_PATH)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(ReportDto.class);
    }

    public Mono<ReportDto> update(Integer id, ReportDto dto) {
        return coreServiceWebClient.put()
                .uri(BASE_PATH + "/{id}", id)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(ReportDto.class);
    }

    public Mono<Void> disable(Integer id) {
        return coreServiceWebClient.put()
                .uri(BASE_PATH + "/disable/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> restore(Integer id) {
        return coreServiceWebClient.put()
                .uri(BASE_PATH + "/restore/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> delete(Integer id) {
        return coreServiceWebClient.delete()
                .uri(BASE_PATH + "/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Boolean> existsByYearAndTrimester(Integer year, String trimester) {
        return coreServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/exist")
                        .queryParam("year", year)
                        .queryParam("trimester", trimester)
                        .build())
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
