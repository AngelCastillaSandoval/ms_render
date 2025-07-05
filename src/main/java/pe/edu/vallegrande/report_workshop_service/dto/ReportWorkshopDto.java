package pe.edu.vallegrande.report_workshop_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos que necesitas mostrar o consumir desde el cliente
 * Se usa para enviar o recibir datos entre capas internas o exponerlos al frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportWorkshopDto {
    private Integer id;
    private Integer reportId;
    private Integer workshopId;
    private String workshopName;
    private LocalDate workshopDateStart;
    private LocalDate workshopDateEnd;
    private String description;
    private String[] imageUrl;
    private String workshopStatus;
    private List<ReportAttendanceSummaryDto> attendanceSummaries;
}
