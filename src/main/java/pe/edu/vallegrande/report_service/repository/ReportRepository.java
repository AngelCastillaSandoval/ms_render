package pe.edu.vallegrande.report_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_service.model.Report;
import reactor.core.publisher.Flux;

@Repository
public interface ReportRepository extends ReactiveCrudRepository<Report, Integer> {
    // Reportes por año y trimestre (evitar duplicados)
    Flux<Report> findAllByYearAndTrimester(Integer year, String trimester);
}
