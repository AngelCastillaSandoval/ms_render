package pe.edu.vallegrande.report_workshop_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.service.ReportWorkshopService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reports-workshop")
@RequiredArgsConstructor
public class ReportController {

    private final ReportWorkshopService service;

    /**
     * 🔹 Listar todos los reportes con filtros opcionales
     */
    @GetMapping
    public Flux<ReportWithWorkshopsDto> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trimester,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateEnd
    ) {
        return service.findFilteredReports(status, trimester, year, workshopDateStart, workshopDateEnd);
    }

    /**
     * 🔹 Obtener un reporte por ID con filtros de fecha
     */
    @GetMapping("/{id}/filtered")
    public Mono<ReportWithWorkshopsDto> getByIdWithFilter(
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateEnd
    ) {
        return service.findByIdWithDateFilter(id, workshopDateStart, workshopDateEnd);
    }

    /**
     * ✅ Crear un nuevo reporte
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReportWithWorkshopsDto> createReport(@Valid @RequestBody ReportWithWorkshopsDto dto) {
        return service.create(dto);
    }

    /**
     * 🛠️ Editar un reporte existente
     */
    @PutMapping("/{id}")
    public Mono<ReportWithWorkshopsDto> updateReport(@PathVariable Integer id, @Valid @RequestBody ReportWithWorkshopsDto dto) {
        return service.update(id, dto);
    }

    /**
     * ❌ Eliminación lógica (desactivar)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> disableReport(@PathVariable Integer id) {
        return service.disable(id);
    }

    /**
     * ♻️ Restaurar un reporte previamente desactivado
     */
    @PutMapping("/restore/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> restoreReport(@PathVariable Integer id) {
        return service.restore(id);
    }

    /**
     * ☠️ Eliminación física (si decides activarla)
     */
    @DeleteMapping("/hard-delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteReportCompletely(@PathVariable Integer id) {
        return service.delete(id);
    }

    /**
     * 📄 Generar PDF con filtro por fechas
     */
    @GetMapping("/{reportId}/pdf")
    public Mono<ResponseEntity<byte[]>> generatePdf(
            @PathVariable Integer reportId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateEnd
    ) {
        return service.generatePdfByIdWithDateFilter(reportId, workshopDateStart, workshopDateEnd);
    }
}
