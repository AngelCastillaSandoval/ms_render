package pe.edu.vallegrande.report_workshop_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_workshop_service.model.WorkshopCache;
import reactor.core.publisher.Flux;

@Repository
public interface WorkshopCacheRepository extends ReactiveCrudRepository<WorkshopCache, Integer> {

    // 🔍 Listar todos los talleres activos
    Flux<WorkshopCache> findByStatus(String status);
}

