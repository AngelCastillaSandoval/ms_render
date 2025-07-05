package pe.edu.vallegrande.report_workshop_service.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.report_workshop_service.model.ReportAttendanceSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReportAttendanceSummaryRepository extends ReactiveCrudRepository<ReportAttendanceSummary, Long> {

    // Lista los resúmenes vinculados a un taller de reporte
    Flux<ReportAttendanceSummary> findByReportWorkshopId(Integer reportWorkshopId);

    // Elimina múltiples resúmenes según los IDs del taller de reporte
    @Query("DELETE FROM report_attendance_summary WHERE report_workshop_id IN (:ids)")
    Mono<Void> deleteByReportWorkshopIdIn(List<Integer> ids);
}
