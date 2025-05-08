package pe.edu.vallegrande.actividad.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("activity_log")
public class ActivityLog {
    @Id
    private UUID id;
    private String imagen;
    private String nombre;
    private String modulo;
    private String accion;
    private LocalDateTime fecha;
}

