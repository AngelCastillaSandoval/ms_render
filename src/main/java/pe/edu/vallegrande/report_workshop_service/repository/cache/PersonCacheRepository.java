package pe.edu.vallegrande.report_workshop_service.repository.cache;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_workshop_service.model.PersonCache;
import reactor.core.publisher.Mono;

@Repository
public interface PersonCacheRepository extends ReactiveCrudRepository<PersonCache, Integer> {

    // Buscar persona por ID
    Mono<PersonCache> findById(Integer id);

}
