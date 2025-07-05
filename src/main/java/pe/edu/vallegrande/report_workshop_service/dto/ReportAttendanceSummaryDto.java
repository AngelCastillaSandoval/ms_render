package pe.edu.vallegrande.report_workshop_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para representar resúmenes de asistencia que ya han sido insertados
 * en la tabla 'report_attendance_summary'.
 * Datos que necesitas mostrar o consumir desde el cliente
 * Se usa para enviar o recibir datos entre capas internas o exponerlos al frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAttendanceSummaryDto {
    private Integer id;
    private Integer reportWorkshopId;
    private Integer personId;
    private String personName;
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
    private Integer justifiedCount;
}

