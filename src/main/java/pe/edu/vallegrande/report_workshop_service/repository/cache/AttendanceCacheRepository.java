package pe.edu.vallegrande.report_workshop_service.repository.cache;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_workshop_service.model.AttendanceCache;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface AttendanceCacheRepository extends ReactiveCrudRepository<AttendanceCache, Integer> {

    // Buscar asistencias por temas e ID de persona
    Flux<AttendanceCache> findAllByIssueIdInAndPersonId(List<Integer> issueIds, Integer personId);
}
