package pe.edu.vallegrande.report_workshop_service.repository.cache;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_workshop_service.model.WorkshopCache;

@Repository
public interface WorkshopCacheRepository extends ReactiveCrudRepository<WorkshopCache, Integer> {
    // CRUD reactivo para workshops en caché
}
