package pe.edu.vallegrande.report_workshop_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.model.PersonCache;
import pe.edu.vallegrande.report_workshop_service.model.event.PersonEvent;
import pe.edu.vallegrande.report_workshop_service.repository.cache.PersonCacheRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonEventConsumer {

    private final PersonCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;

    /**
     * Escucha eventos del topic "person-events" y sincroniza la tabla person_cache.
     */
    @KafkaListener(
            topics = "${person-kafka.topic}",
            groupId = "${person-kafka.group-id}",
            containerFactory = "personKafkaListenerContainerFactory"
    )
    public void consumePersonEvent(PersonEvent dto) {
        try {
            if (dto.getIdPerson() == null || dto.getEventType() == null) {
                log.warn("Evento ignorado por datos incompletos: {}", dto);
                return;
            }

            log.info("Evento Person recibido: {}", dto);

            switch (dto.getEventType()) {
                case "CREATED", "UPDATED" -> {
                    PersonCache cache = PersonCache.builder()
                            .idPerson(dto.getIdPerson())
                            .name(dto.getName())
                            .surname(dto.getSurname())
                            .age(dto.getAge())
                            .typeKinship(dto.getTypeKinship())
                            .sponsored(dto.getSponsored())
                            .state(dto.getState())
                            .familyIdFamily(dto.getFamilyIdFamily())
                            .build();

                    cacheRepository.findById(dto.getIdPerson())
                            .flatMap(existing -> {
                                existing.setName(cache.getName());
                                existing.setSurname(cache.getSurname());
                                existing.setAge(cache.getAge());
                                existing.setTypeKinship(cache.getTypeKinship());
                                existing.setSponsored(cache.getSponsored());
                                existing.setState(cache.getState());
                                existing.setFamilyIdFamily(cache.getFamilyIdFamily());
                                return cacheRepository.save(existing);
                            })
                            .switchIfEmpty(Mono.defer(() -> template.insert(PersonCache.class).using(cache)))
                            .subscribe(saved -> log.info("PersonCache insertado/actualizado: {}", saved));
                }

                case "DELETED" -> {
                    cacheRepository.deleteById(dto.getIdPerson())
                            .subscribe(unused -> log.info("🗑️ PersonCache eliminado ID: {}", dto.getIdPerson()));
                }

                default -> log.warn("Tipo de evento desconocido: {}", dto.getEventType());
            }

        } catch (Exception e) {
            log.error("Error al procesar evento de persona: {}", e.getMessage(), e);
        }
    }
}
