package pe.edu.vallegrande.report_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportDto {

    private Integer id;

    @NotNull(message = "El año es obligatorio")
    private Integer year;

    @NotBlank(message = "El trimestre es obligatorio")
    private String trimester;

    @NotBlank(message = "La descripción es obligatoria")
    private String descriptionUrl;

    private String scheduleUrl;

    private String status;
}
