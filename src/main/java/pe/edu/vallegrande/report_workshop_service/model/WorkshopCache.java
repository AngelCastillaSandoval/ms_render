package pe.edu.vallegrande.report_workshop_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @Column("start_date")
    private LocalDate startDate;
    @NotNull(message = "La fecha de fin no puede ser nula")
    @Column("end_date")
    private LocalDate endDate;
    @NotBlank(message = "El estado del taller es obligatorio")
    @Pattern(regexp = "[AI]", message = "El estado debe ser 'A' o 'I'")
    private String state;
    @NotBlank(message = "El campo personId no puede estar vacío")
    @Column("person_id")
    private String personId;

}