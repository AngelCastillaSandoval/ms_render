package pe.edu.vallegrande.report_workshop_service.model;

import jakarta.validation.constraints.NotNull;
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
@Table("report_workshops")
public class ReportWorkshop {

    @Id
    private Integer id;
    @NotNull(message = "El ID del reporte no puede ser nulo")
    @Column("report_id")
    private Integer reportId;
    @Column("workshop_id")
    private Integer workshopId;
    @Column("workshop_name")
    private String workshopName;
    @Column("workshop_date_start")
    private LocalDate workshopDateStart;
    @Column("workshop_date_end")
    private LocalDate workshopDateEnd;
    private String description;
    @NotNull(message = "Debe especificar al menos una imagen")
    @Column("image_urls")
    private String[] imageUrl;
}
