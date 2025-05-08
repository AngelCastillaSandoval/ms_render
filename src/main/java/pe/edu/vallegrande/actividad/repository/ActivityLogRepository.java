package pe.edu.vallegrande.actividad.repository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.actividad.model.ActivityLog;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ActivityLogRepository extends ReactiveCrudRepository<ActivityLog, UUID> {
    Flux<ActivityLog> findAllByOrderByFechaDesc();
}