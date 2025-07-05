package pe.edu.vallegrande.report_workshop_service.model.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Representa un evento Kafka
 * Los eventos deben ser independientes del modelo de base de datos.
 */
@Data
public class IssueEvent {
    private Integer id;
    private String name;
    private Integer workshopId;
    private String sesion;
    private LocalDateTime scheduledTime;
    private String state;
}
