package pe.edu.vallegrande.report_workshop_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_workshop_service.model.ReportWorkshop;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReportWorkshopRepository extends ReactiveCrudRepository<ReportWorkshop, Integer> {

    // 🔍 Obtener talleres por ID de reporte
    Flux<ReportWorkshop> findByReportId(Integer reportId);

    // ❌ Eliminar todos los talleres al editar un reporte
    Mono<Void> deleteByReportId(Integer reportId);
}
