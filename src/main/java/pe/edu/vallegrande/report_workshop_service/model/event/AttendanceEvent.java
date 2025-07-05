package pe.edu.vallegrande.report_workshop_service.model.event;

import lombok.Data;

import java.time.LocalDateTime;


/**
 * Representa un evento Kafka
 * Los eventos deben ser independientes del modelo de base de datos.
 */
@Data
public class AttendanceEvent {
    private Integer id;
    private Integer issueId;
    private Integer personId;
    private LocalDateTime entryTime;
    private String record;
    private String justificationDocument;
    private String state;
    private String eventType;
}
