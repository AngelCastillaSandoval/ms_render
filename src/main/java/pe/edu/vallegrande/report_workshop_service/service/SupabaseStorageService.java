package pe.edu.vallegrande.report_workshop_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String bucket;
    private final String projectUrl;

    public SupabaseStorageService(
            @Value("${supabase.project-url}") String projectUrl,
            @Value("${supabase.api-key}") String apiKey,
            @Value("${supabase.bucket}") String bucket
    ) {
        this.projectUrl = projectUrl;
        this.bucket = bucket;
        this.webClient = WebClient.builder()
                .baseUrl(projectUrl + "/storage/v1")
                .defaultHeader("apikey", apiKey)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public Mono<String> uploadPdf(String folder, String fileName, byte[] pdfBytes) {
        String path = folder + "/" + fileName;
        return webClient.put()
                .uri(uriBuilder -> uriBuilder.path("/object/{bucket}/{path}")
                        .build(bucket, path))
                .header("x-upsert", "true")
                .contentType(MediaType.APPLICATION_PDF)
                .body(BodyInserters.fromValue(pdfBytes))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> projectUrl + "/storage/v1/object/public/" + bucket + "/" + path);
    }

    public Mono<Boolean> fileExists(String folder, String fileName) {
        String path = folder + "/" + fileName;
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/object/info/{bucket}/{path}")
                        .build(bucket, path))
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> true)
                .onErrorResume(err -> Mono.just(false));
    }

    public String getPublicUrl(String folder, String fileName) {
        return projectUrl + "/storage/v1/object/public/" + bucket + "/" + folder + "/" + fileName;
    }


}
