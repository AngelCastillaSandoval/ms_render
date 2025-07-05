package pe.edu.vallegrande.report_workshop_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Datos que necesitas mostrar o consumir desde el cliente
 * Se usa para enviar o recibir datos entre capas internas o exponerlos al frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportWithWorkshopsDto {
    private ReportDto report;
    private List<ReportWorkshopDto> workshops;
}
