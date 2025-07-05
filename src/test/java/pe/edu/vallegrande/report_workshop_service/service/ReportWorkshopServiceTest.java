package pe.edu.vallegrande.report_workshop_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.report_workshop_service.dto.ReportDto;
import pe.edu.vallegrande.report_workshop_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_workshop_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_workshop_service.webclient.ReportCoreClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportWorkshopServiceTest {

    @InjectMocks
    private ReportWorkshopService service;

    @Mock
    private ReportCoreClient reportClient;

    @Mock
    private ReportWorkshopRepository reportWorkshopRepo;


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
