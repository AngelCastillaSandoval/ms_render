package pe.edu.vallegrande.report_workshop_service.repository.cache;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pe.edu.vallegrande.report_workshop_service.model.IssueCache;
import reactor.core.publisher.Flux;

public interface IssueCacheRepository extends ReactiveCrudRepository<IssueCache, Integer> {
    // Buscar temas por ID de taller
    Flux<IssueCache> findAllByWorkshopId(Integer workshopId);
}
