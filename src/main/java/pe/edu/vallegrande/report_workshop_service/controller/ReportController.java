package pe.edu.vallegrande.report_workshop_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.service.ReportWorkshopService;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.GoogleSlidesService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/reports-workshop")
@RequiredArgsConstructor
public class ReportController {

    private final ReportWorkshopService service;
    private final GoogleSlidesService googleSlidesService;

    /**
     * Listar todos los reportes con filtros opcionales
     */
    @GetMapping
    public Flux<ReportWithWorkshopsDto> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trimester,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateEnd
    ) {
        log.info("Obteniendo reportes con filtros - status: {}, trimester: {}, year: {}", status, trimester, year);
        return service.findFilteredReports(status, trimester, year, workshopDateStart, workshopDateEnd)
                .doOnError(error -> log.error("Error al obtener reportes filtrados: {}", error.getMessage()));
    }

    /**
     * Obtener un reporte por ID con filtros de fecha
     */
    @GetMapping("/{id}/filtered")
    public Mono<ReportWithWorkshopsDto> getByIdWithFilter(
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workshopDateEnd
    ) {
        log.info("Obteniendo reporte ID {} con filtros de fecha", id);
        return service.findByIdWithDateFilter(id, workshopDateStart, workshopDateEnd)
                .doOnError(error -> log.error("Error al obtener reporte ID {}: {}", id, error.getMessage()));
    }

    /**
     * Crear un nuevo reporte
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReportWithWorkshopsDto> createReport(@Valid @RequestBody ReportWithWorkshopsDto dto) {
        log.info("Creando nuevo reporte");
        return service.create(dto)
                .doOnSuccess(result -> log.info("Reporte creado exitosamente con ID: {}", result.getReport().getId()))
                .doOnError(error -> log.error("Error al crear reporte: {}", error.getMessage()));
    }

    /**
     * 🛠Editar un reporte existente
     */
    @PutMapping("/{id}")
    public Mono<ReportWithWorkshopsDto> updateReport(@PathVariable Integer id, @Valid @RequestBody ReportWithWorkshopsDto dto) {
        log.info("Actualizando reporte ID: {}", id);
        return service.update(id, dto)
                .doOnSuccess(result -> log.info("Reporte ID {} actualizado exitosamente", id))
                .doOnError(error -> log.error("Error al actualizar reporte ID {}: {}", id, error.getMessage()));
    }

    /**
     * Eliminación lógica (desactivar)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> disableReport(@PathVariable Integer id) {
        log.info("Desactivando reporte ID: {}", id);
        return service.disable(id)
                .doOnSuccess(result -> log.info("Reporte ID {} desactivado exitosamente", id))
                .doOnError(error -> log.error("Error al desactivar reporte ID {}: {}", id, error.getMessage()));
    }

    /**
     * Restaurar un reporte previamente desactivado
     */
    @PutMapping("/restore/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> restoreReport(@PathVariable Integer id) {
        log.info("Restaurando reporte ID: {}", id);
        return service.restore(id)
                .doOnSuccess(result -> log.info("Reporte ID {} restaurado exitosamente", id))
                .doOnError(error -> log.error("Error al restaurar reporte ID {}: {}", id, error.getMessage()));
    }

    /**
     * Eliminación física
     */
    @DeleteMapping("/hard-delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteReportCompletely(@PathVariable Integer id) {
        log.warn("Eliminación física del reporte ID: {}", id);
        return service.delete(id)
                .doOnSuccess(result -> log.info("Reporte ID {} eliminado físicamente", id))
                .doOnError(error -> log.error("Error al eliminar físicamente reporte ID {}: {}", id, error.getMessage()));
    }

    /**
     * Exportar reporte como PDF o PPTX
     */
    @GetMapping("/export/{tipo}/{id}")
    public Mono<ResponseEntity<byte[]>> exportarArchivo(
            @PathVariable String tipo,
            @PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return service.findByIdWithDateFilter(id, startDate, endDate)
                .flatMap(dto -> googleSlidesService.exportFile(dto, tipo, id, startDate, endDate))
                .onErrorResume(e -> {
                    String msg = "Error al exportar archivo: " + e.getMessage();
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(msg.getBytes()));
                });
    }
}
