package pe.edu.vallegrande.report_service.model;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("reports")
public class Report {
    @Id
    private Integer id;

    @NotNull(message = "El año es obligatorio")
    private Integer year;

    @NotBlank(message = "El trimestre es obligatorio")
    private String trimester;

    @Column("description_url")
    @NotBlank(message = "La descripción es obligatoria")
    private String descriptionUrl;

    @Column("schedule_url")
    private String scheduleUrl;

    @Builder.Default
    private String status = "A";
}