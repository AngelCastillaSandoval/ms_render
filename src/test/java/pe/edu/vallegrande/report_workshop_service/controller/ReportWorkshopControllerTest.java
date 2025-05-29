package pe.edu.vallegrande.report_workshop_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.report_workshop_service.dto.ReportDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_workshop_service.service.ReportWorkshopService;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportWorkshopControllerTest {

    @Mock
    private ReportWorkshopService service;

    @InjectMocks
    private ReportController controller;

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    /**
     * Test para verificar que la creación de un reporte con talleres personalizados
     * devuelva un estado HTTP 201 Created y el contenido esperado en el body (año y nombre del taller).
     */
    @Test
    void createReport_shouldReturnCreatedReport() {
        ReportDto report = new ReportDto();
        report.setId(1);
        report.setYear(2024);
        report.setTrimester("https://supabase.com/reports/html/abril-junio.html");
        report.setDescriptionUrl("desc");
        report.setStatus("A");

        ReportWorkshopDto workshop = new ReportWorkshopDto();
        workshop.setWorkshopName("Taller de arte");
        workshop.setWorkshopDateStart(LocalDate.of(2024, 4, 10));
        workshop.setWorkshopDateEnd(LocalDate.of(2024, 4, 12));
        workshop.setDescription("Actividad creativa");
        workshop.setImageUrl(new String[]{
                "https://supabase.com/storage/reports/arte1.jpg"
        });

        ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
        dto.setReport(report);
        dto.setWorkshops(List.of(workshop));

        // Simula el comportamiento del service al crear
        when(service.create(dto)).thenReturn(Mono.just(dto));

        // Realiza la petición POST y verifica los campos clave en la respuesta
        webTestClient.post()
                .uri("/api/reports")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.report.year").isEqualTo(2024)
                .jsonPath("$.workshops[0].workshopName").isEqualTo("Taller de arte");
    }

    /**
     * Test para verificar que la actualización de un reporte
     * devuelve un estado HTTP 200 OK y que los nuevos valores están reflejados en el cuerpo de la respuesta.
     */
    @Test
    void updateReport_shouldReturnUpdatedReport() {
        ReportDto report = new ReportDto();
        report.setId(1);
        report.setYear(2024);
        report.setTrimester("abril-junio");
        report.setDescriptionUrl("https://supabase.com/reports/html/abril-junio.html");
        report.setStatus("A");

        ReportWorkshopDto workshop = new ReportWorkshopDto();
        workshop.setWorkshopName("Taller actualizado");
        workshop.setWorkshopDateStart(LocalDate.of(2024, 4, 20));
        workshop.setWorkshopDateEnd(LocalDate.of(2024, 4, 22));
        workshop.setDescription("desc nueva");
        workshop.setImageUrl(new String[]{
                "https://supabase.com/storage/reports/nueva.jpg"
        });

        ReportWithWorkshopsDto updatedDto = new ReportWithWorkshopsDto();
        updatedDto.setReport(report);
        updatedDto.setWorkshops(List.of(workshop));

        // Simula el comportamiento del service al actualizar
        when(service.update(1, updatedDto)).thenReturn(Mono.just(updatedDto));

        // Realiza la petición PUT y verifica que la descripción y nombre del taller hayan sido actualizados
        webTestClient.put()
                .uri("/api/reports/1")
                .bodyValue(updatedDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.report.description").isEqualTo("https://supabase.com/reports/html/abril-junio.html")
                .jsonPath("$.workshops[0].workshopName").isEqualTo("Taller actualizado");
    }

    /**
     * Test para verificar que el endpoint de restauración de un reporte
     * devuelve correctamente el estado HTTP 204 No Content.
     */
    @Test
    void restoreReport_shouldReturnNoContent() {
        // Simula una restauración vacía (Mono.empty())
        when(service.restore(1)).thenReturn(Mono.empty());

        // Realiza la petición PUT y espera un estado 204
        webTestClient.put()
                .uri("/api/reports/restore/1")
                .exchange()
                .expectStatus().isNoContent();
    }
}
