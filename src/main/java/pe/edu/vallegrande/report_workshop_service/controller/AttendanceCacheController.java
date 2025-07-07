package pe.edu.vallegrande.report_workshop_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.report_workshop_service.dto.PreviewAttendanceSummaryDto;
import pe.edu.vallegrande.report_workshop_service.service.AttendanceCacheService;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/attendance-preview")
@RequiredArgsConstructor
public class AttendanceCacheController {

    private final AttendanceCacheService attendanceCacheService;

    /**
     * Obtener resumen de asistencia de un taller, sin necesidad de crear un reporte aún
     */
    @GetMapping("/workshop/{workshopId}")
    public Flux<PreviewAttendanceSummaryDto> getPreview(@PathVariable Integer workshopId) {
        return attendanceCacheService.getPreviewSummaryByWorkshopId(workshopId);
    }
}
