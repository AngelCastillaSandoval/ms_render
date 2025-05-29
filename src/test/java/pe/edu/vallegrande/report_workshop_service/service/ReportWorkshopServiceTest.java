package pe.edu.vallegrande.report_workshop_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.report_workshop_service.dto.ReportDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_workshop_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_workshop_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_workshop_service.repository.WorkshopCacheRepository;
import pe.edu.vallegrande.report_workshop_service.webclient.ReportCoreClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportWorkshopServiceTest {

    @InjectMocks
    private ReportWorkshopService service;

    @Mock
    private ReportCoreClient reportClient;

    @Mock
    private ReportWorkshopRepository reportWorkshopRepo;

    @Mock
    private WorkshopCacheRepository workshopCacheRepo;

    @Mock
    private SupabaseStorageService storageService;

    /**
     * ✅ Prueba la creación de un reporte con un taller personalizado (sin workshopId).
     * Se mockea el cliente core para crear el reporte y el repo para guardar los talleres.
     * Se verifica que se devuelve correctamente el DTO completo.
     */
    @Test
    void createReportWithCustomWorkshops_shouldReturnSavedReportWithWorkshops() {
        // 🔸 Datos del reporte original
        ReportDto report = new ReportDto();
        report.setId(1);
        report.setYear(2024);
        report.setTrimester("abril-junio");
        report.setDescriptionUrl("https://supabase.com/reports/html/abril-junio.html");
        report.setStatus("A");

        // 🔸 Taller personalizado con imágenes de Supabase
        ReportWorkshopDto workshopDto = new ReportWorkshopDto();
        workshopDto.setWorkshopName("Taller de manualidades");
        workshopDto.setWorkshopDateStart(LocalDate.of(2024, 4, 5));
        workshopDto.setWorkshopDateEnd(LocalDate.of(2024, 4, 7));
        workshopDto.setDescription("Se elaboraron figuras con materiales reciclables.");
        workshopDto.setImageUrl(new String[] {
                "https://xyz.supabase.co/storage/v1/object/public/reports/img1.jpg",
                "https://xyz.supabase.co/storage/v1/object/public/reports/img2.jpg"
        });

        // 🔸 DTO completo a insertar
        ReportWithWorkshopsDto input = new ReportWithWorkshopsDto();
        input.setReport(report);
        input.setWorkshops(List.of(workshopDto));

        // 🔸 Mock del reporte ya guardado
        ReportDto savedReport = new ReportDto();
        savedReport.setId(1);
        savedReport.setYear(2024);
        savedReport.setTrimester("abril-junio");
        savedReport.setDescriptionUrl("Reporte trimestral");
        savedReport.setStatus("A");

        // 🔸 Mock del taller guardado
        ReportWorkshop savedWorkshop = ReportWorkshop.builder()
                .id(1)
                .reportId(1)
                .workshopName("Taller de manualidades")
                .description("Se elaboraron figuras con materiales reciclables.")
                .imageUrl(new String[] {
                        "https://xyz.supabase.co/storage/v1/object/public/reports/img1.jpg",
                        "https://xyz.supabase.co/storage/v1/object/public/reports/img2.jpg"
                })
                .build();

        // 🔸 Mocks del comportamiento esperado
        when(reportClient.create(report)).thenReturn(Mono.just(savedReport));
        when(reportWorkshopRepo.saveAll(anyList())).thenReturn(Flux.just(savedWorkshop));

        // 🔸 Verificación
        StepVerifier.create(service.create(input))
                .expectNextMatches(result ->
                        result.getReport().getId().equals(1) &&
                                result.getWorkshops().size() == 1 &&
                                result.getWorkshops().get(0).getWorkshopName().equals("Taller de manualidades"))
                .verifyComplete();
    }

    /**
     * ✅ Prueba la restauración de un reporte eliminado.
     * Se espera que el cliente core sea llamado correctamente y no haya error.
     */
    @Test
    void restoreReport_shouldCallCoreService() {
        // 🔸 Mock: el core devuelve vacío (void)
        when(reportClient.restore(5)).thenReturn(Mono.empty());

        // 🔸 Verificación
        StepVerifier.create(service.restore(5))
                .verifyComplete();

        // 🔸 Verifica que se haya llamado exactamente una vez
        verify(reportClient).restore(5);
    }

    /**
     * ✅ Prueba la lógica de filtrado de reportes por estado, trimestre y año.
     * Se devuelve un taller vinculado al reporte para verificar que se asocia correctamente.
     */
    @Test
    void findFilteredReports_shouldReturnReportsMatchingYearAndTrimester() {
        // 🔸 Reporte existente en el core
        ReportDto report = new ReportDto();
        report.setId(2);
        report.setYear(2024);
        report.setTrimester("abril-junio");
        report.setDescriptionUrl("https://supabase.com/reports/html/abril-junio.html");
        report.setStatus("A");

        // 🔸 Taller relacionado
        ReportWorkshop workshop = ReportWorkshop.builder()
                .id(1)
                .reportId(2)
                .workshopName("Taller de dibujo")
                .workshopDateStart(LocalDate.of(2024, 5, 10))
                .imageUrl(new String[] {
                        "https://xyz.supabase.co/storage/v1/object/public/reports/dibujo1.jpg"
                })
                .build();

        // 🔸 Mocks de llamadas
        when(reportClient.findAll()).thenReturn(Flux.just(report));
        when(reportWorkshopRepo.findByReportId(2)).thenReturn(Flux.just(workshop));

        // 🔸 Verificación
        StepVerifier.create(service.findFilteredReports("A", "abril-junio", 2024, null, null))
                .expectNextMatches(result ->
                        result.getReport().getId().equals(2) &&
                                result.getWorkshops().size() == 1 &&
                                result.getWorkshops().get(0).getWorkshopName().equals("Taller de dibujo"))
                .verifyComplete();
    }
}
