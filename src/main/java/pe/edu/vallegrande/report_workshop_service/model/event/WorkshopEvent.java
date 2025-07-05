package pe.edu.vallegrande.report_workshop_service.model.event;

import lombok.Data;

import java.time.LocalDate;

/**
 * Representa un evento Kafka
 * Los eventos deben ser independientes del modelo de base de datos.
 */
@Data
public class WorkshopEvent {
    private Integer id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String state;
    private String personId;
}
