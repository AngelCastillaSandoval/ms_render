package pe.edu.vallegrande.report_workshop_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.model.WorkshopCache;
import pe.edu.vallegrande.report_workshop_service.model.event.WorkshopEvent;
import pe.edu.vallegrande.report_workshop_service.repository.cache.WorkshopCacheRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkshopEventConsumer {

    private final WorkshopCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;

    /**
     * Escucha eventos del topic "workshop-events" y sincroniza la tabla workshop_cache.
     */
    @KafkaListener(
            topics = "${workshop-kafka.topic}",
            groupId = "${workshop-kafka.group-id}",
            containerFactory = "workshopKafkaListenerContainerFactory"
    )
    public void consumeWorkshopEvent(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            WorkshopEvent dto = objectMapper.readValue(json, WorkshopEvent.class);

            if (dto.getId() == null || dto.getName() == null) {
                log.warn("Evento ignorado por datos incompletos: {}", dto);
                return;
            }

            log.info("Recibido evento Kafka: {}", dto);

            WorkshopCache cache = WorkshopCache.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .personId(dto.getPersonId())
                    .state(dto.getState())
                    .build();

            // Actualiza si existe, inserta si no
            cacheRepository.findById(dto.getId())
                    .flatMap(existing -> {
                        existing.setName(cache.getName());
                        existing.setStartDate(cache.getStartDate());
                        existing.setEndDate(cache.getEndDate());
                        existing.setPersonId(cache.getPersonId());
                        existing.setState(cache.getState());
                        return cacheRepository.save(existing);
                    })
                    .switchIfEmpty(template.insert(WorkshopCache.class).using(cache))
                    .subscribe(
                            saved -> log.info("WorkshopCache insertado/actualizado: {}", saved),
                            error -> log.error("Error al guardar WorkshopCache: {}", error.getMessage(), error)
                    );

        } catch (Exception e) {
            log.error("Error procesando evento Kafka: {}", e.getMessage(), e);
        }
    }
}
