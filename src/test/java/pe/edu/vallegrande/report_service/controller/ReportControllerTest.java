package pe.edu.vallegrande.report_service.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.vallegrande.report_service.config.DotenvInitializer;
import pe.edu.vallegrande.report_service.config.TestSecurityConfig;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.service.ReportService;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ContextConfiguration(initializers = DotenvInitializer.class)
@AutoConfigureWebTestClient
@Import({ReportControllerTest.MockConfig.class, TestSecurityConfig.class})
public class ReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReportService reportService;

    /**
     * Configura un mock manual de ReportService usando Mockito.
     */
    @TestConfiguration
    static class MockConfig {
        @Bean
        public ReportService reportService() {
            return Mockito.mock(ReportService.class);
        }
    }

    @Test
    void shouldReturn400_whenMissingTrimester() {
        // 🧪 TEST 1: Devuelve 400 Bad Request si falta el campo "trimester"

        ReportDto dto = new ReportDto();
        dto.setYear(2025);
        dto.setDescriptionUrl("https://supabase.com/reports/html/sin-trimestre.html");
        dto.setScheduleUrl("https://supabase.com/reports/schedules/sin-trimestre.jpg");

        webTestClient.post()
                .uri("/api/reports")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    assert result.getResponseBody().containsKey("trimester");
                });
    }

    @Test
    void shouldReturn201_whenValidReportIsSaved() {
        // 🧪 TEST 2: Guarda un reporte válido correctamente (HTTP 201)

        ReportDto dto = new ReportDto();
        dto.setYear(2024);
        dto.setTrimester("Abril-Junio");
        dto.setDescriptionUrl("https://supabase.com/reports/html/abril-junio.html");
        dto.setScheduleUrl("https://supabase.com/reports/schedules/abril-junio.jpg");
        dto.setStatus("A");

        Mockito.when(reportService.save(any())).thenReturn(Mono.just(dto));

        webTestClient.post()
                .uri("/api/reports")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.year").isEqualTo(2024)
                .jsonPath("$.trimester").isEqualTo("Abril-Junio");
    }

    @Test
    void shouldReturn404_whenNotFoundById() {
        // 🧪 TEST 3: Devuelve 404 si no se encuentra el ID

        Mockito.when(reportService.findById(99)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/reports/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    Map<String, Object> body = result.getResponseBody();
                    assert body != null;
                    assert body.containsKey("message");
                    assert body.get("message").equals("Reporte con ID 99 no encontrado");

                    // ✅ Mostrar mensaje en consola
                    System.out.println("✅ Mensaje recibido: " + body.get("message"));
                });
    }
}
