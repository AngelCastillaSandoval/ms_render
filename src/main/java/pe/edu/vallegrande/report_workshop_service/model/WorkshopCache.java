package pe.edu.vallegrande.report_workshop_service.model;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("workshop_cache")
public class WorkshopCache {

    @Id
    private Integer id;

    @NotBlank(message = "El nombre del taller no puede estar vacío")
    private String name;

    @NotNull(message = "La fecha de inicio no puede ser nula")
    @Column("date_start")
    private LocalDate dateStart;

    @NotNull(message = "La fecha de fin no puede ser nula")
    @Column("date_end")
    private LocalDate dateEnd;

    @NotBlank(message = "El estado del taller es obligatorio")
    @Pattern(regexp = "[AI]", message = "El estado debe ser 'A' o 'I'")
    private String status;
}