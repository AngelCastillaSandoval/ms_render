package pe.edu.vallegrande.report_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReportServiceTest {

    private ReportRepository reportRepository;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportService = new ReportService(reportRepository);
    }

    @Test
    void shouldSaveReport_whenValidData() {
        // 💾 Insertar nuevo reporte cuando no hay duplicado activo

        ReportDto dto = new ReportDto();
        dto.setYear(2025);
        dto.setTrimester("Enero-Marzo");
        dto.setDescriptionUrl("https://supabase.com/reports/html/enero-marzo.html");
        dto.setScheduleUrl("https://supabase.com/reports/schedules/enero-marzo.jpg");

        when(reportRepository.findAllByYearAndTrimester(anyInt(), anyString()))
                .thenReturn(Flux.empty());
        when(reportRepository.save(any()))
                .thenReturn(Mono.just(toEntity(dto)));

        StepVerifier.create(reportService.save(dto))
                .expectNextMatches(saved ->
                        saved.getYear().equals(2025) &&
                                saved.getTrimester().equals("Enero-Marzo")
                )
                .verifyComplete();
    }

    @Test
    void shouldRestoreReport_whenExists() {
        // 🔁 Restaurar un reporte inactivo

        Integer id = 99;

        Report inactivo = Report.builder()
                .id(id)
                .year(2023)
                .trimester("Abril-Junio")
                .descriptionUrl("https://supabase.com/reports/html/abril-junio.html")
                .scheduleUrl("https://supabase.com/reports/schedules/abril-junio.jpg")
                .status("I")
                .build();

        Report activo = inactivo.toBuilder().status("A").build();

        when(reportRepository.findById(id)).thenReturn(Mono.just(inactivo));
        when(reportRepository.save(any())).thenReturn(Mono.just(activo));

        StepVerifier.create(reportService.restore(id))
                .verifyComplete();

        verify(reportRepository).save(argThat(report ->
                report.getStatus().equals("A") && report.getId().equals(id)
        ));
    }

    @Test
    void shouldDeletePhysically_whenExists() {
        // 🗑️ Eliminar físicamente un reporte existente

        Report report = Report.builder()
                .id(1)
                .year(2024)
                .trimester("Julio-Septiembre")
                .descriptionUrl("https://supabase.com/reports/html/julio-septiembre.html")
                .scheduleUrl("https://supabase.com/reports/schedules/julio-septiembre.jpg")
                .status("A")
                .build();

        when(reportRepository.findById(1)).thenReturn(Mono.just(report));
        when(reportRepository.delete(report)).thenReturn(Mono.empty());

        StepVerifier.create(reportService.delete(1))
                .verifyComplete();

        verify(reportRepository).delete(report);
    }

    // 🔧 Utilidad local: DTO → Entidad
    private Report toEntity(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .descriptionUrl(dto.getDescriptionUrl())
                .scheduleUrl(dto.getScheduleUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "A")
                .build();
    }
}
