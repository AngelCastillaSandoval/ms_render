package pe.edu.vallegrande.report_workshop_service.model.event;

import lombok.Data;

/**
 * Representa un evento Kafka
 * Los eventos deben ser independientes del modelo de base de datos.
 */
@Data
public class PersonEvent {
    private Integer idPerson;
    private String name;
    private String surname;
    private Integer age;
    private String typeKinship;
    private String sponsored;
    private String state;
    private Integer familyIdFamily;
    private String eventType; // CREATED o UPDATED
}