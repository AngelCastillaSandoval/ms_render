package pe.edu.vallegrande.report_workshop_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.model.AttendanceCache;
import pe.edu.vallegrande.report_workshop_service.model.event.AttendanceEvent;
import pe.edu.vallegrande.report_workshop_service.repository.cache.AttendanceCacheRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceEventConsumer {

    private final AttendanceCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;

    /**
     * Escucha eventos del topic "attendance-events" y sincroniza la tabla attendance_cache.
     */
    @KafkaListener(
            topics = "${attendance-kafka.topic}",
            groupId = "${attendance-kafka.group-id}",
            containerFactory = "attendanceKafkaListenerContainerFactory"
    )
    public void consumeAttendanceEvent(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            AttendanceEvent dto = objectMapper.readValue(json, AttendanceEvent.class);

            if (dto.getId() == null || dto.getEventType() == null) {
                log.warn("Evento ignorado por datos incompletos: {}", dto);
                return;
            }

            log.info("Evento Attendance recibido: {}", dto);

            switch (dto.getEventType()) {
                case "CREATED", "UPDATED" -> {
                    // Inserta o actualiza
                    AttendanceCache cache = AttendanceCache.builder()
                            .id(dto.getId())
                            .issueId(dto.getIssueId())
                            .personId(dto.getPersonId())
                            .entryTime(dto.getEntryTime())
                            .record(dto.getRecord())
                            .justificationDocument(dto.getJustificationDocument())
                            .state(dto.getState())
                            .build();

                    cacheRepository.findById(dto.getId())
                            .flatMap(existing -> {
                                existing.setIssueId(cache.getIssueId());
                                existing.setPersonId(cache.getPersonId());
                                existing.setEntryTime(cache.getEntryTime());
                                existing.setRecord(cache.getRecord());
                                existing.setJustificationDocument(cache.getJustificationDocument());
                                existing.setState(cache.getState());
                                return cacheRepository.save(existing);
                            })
                            .switchIfEmpty(Mono.defer(() -> template.insert(AttendanceCache.class).using(cache)))
                            .subscribe(saved -> log.info("AttendanceCache insertado/actualizado: {}", saved));
                }

                case "DELETED" -> {
                    // Elimina por ID
                    cacheRepository.deleteById(dto.getId())
                            .subscribe(unused -> log.info("AttendanceCache eliminado ID: {}", dto.getId()));
                }

                default -> log.warn("Tipo de evento desconocido: {}", dto.getEventType());
            }

        } catch (Exception e) {
            log.error("Error procesando evento Attendance: {}", e.getMessage(), e);
        }
    }
}
