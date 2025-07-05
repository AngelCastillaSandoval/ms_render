package pe.edu.vallegrande.report_workshop_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para mostrar un resumen de asistencia previo (preview)
 * antes de insertar un Reporte o ReportWorkshop.
 * Este DTO se usa para mostrar, por ejemplo, un resumen inmediato de asistencia
 * al seleccionar un `workshopId` en el formulario del frontend,
 * consultando directamente desde los datos en caché (`attendance_cache`, `person_cache`, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewAttendanceSummaryDto {
    private Integer personId;
    private String personName;
    private Integer presentCount;
    private Integer absentCount;
    private Integer lateCount;
    private Integer justifiedCount;
}
