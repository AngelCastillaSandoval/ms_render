package pe.edu.vallegrande.report_workshop_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.model.IssueCache;
import pe.edu.vallegrande.report_workshop_service.model.event.IssueEvent;
import pe.edu.vallegrande.report_workshop_service.repository.cache.IssueCacheRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueEventConsumer {

    private final IssueCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;

    /**
     * Escucha eventos del topic "issue-events" y sincroniza la tabla issue_cache.
     */
    @KafkaListener(
            topics = "${issue-kafka.topic}",
            groupId = "${issue-kafka.group-id}",
            containerFactory = "issueKafkaListenerContainerFactory"
    )
    public void consumeIssueEvent(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            IssueEvent dto = objectMapper.readValue(json, IssueEvent.class);

            if (dto.getId() == null || dto.getName() == null) {
                log.warn("Evento ignorado por datos incompletos: {}", dto);
                return;
            }

            log.info("Evento Issue recibido: {}", dto);

            // Actualiza si existe, inserta si no
            IssueCache cache = IssueCache.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .workshopId(dto.getWorkshopId())
                    .sesion(dto.getSesion())
                    .scheduledTime(dto.getScheduledTime())
                    .state(dto.getState())
                    .build();

            cacheRepository.findById(dto.getId())
                    .flatMap(existing -> {
                        existing.setName(cache.getName());
                        existing.setWorkshopId(cache.getWorkshopId());
                        existing.setSesion(cache.getSesion());
                        existing.setScheduledTime(cache.getScheduledTime());
                        existing.setState(cache.getState());
                        return cacheRepository.save(existing);
                    })
                    .switchIfEmpty(template.insert(IssueCache.class).using(cache))
                    .subscribe(
                            saved -> log.info("IssueCache insertado/actualizado: {}", saved),
                            error -> log.error("Error al guardar IssueCache: {}", error.getMessage(), error)
                    );

        } catch (Exception e) {
            log.error("Error procesando evento Kafka: {}", e.getMessage(), e);
        }
    }
}
