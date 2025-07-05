package pe.edu.vallegrande.report_workshop_service.service.googleslides;

import com.google.api.services.slides.v1.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_workshop_service.model.ReportAttendanceSummary;
import pe.edu.vallegrande.report_workshop_service.service.SupabaseStorageService;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.editor.GoogleSlidesHtmlEditor;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.editor.GoogleSlidesImageEditor;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.editor.GoogleSlidesTableEditor;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.editor.GoogleSlidesTextEditor;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.exporter.GoogleSlidesExporter;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal que genera presentaciones de Google Slides a partir de datos de reportes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSlidesService {

    private final GoogleAuthService authService;
    private final GoogleSlidesTextEditor textEditor;
    private final GoogleSlidesHtmlEditor htmlEditor;
    private final GoogleSlidesImageEditor imageEditor;
    private final GoogleSlidesTableEditor tableEditor;
    private final GoogleSlidesExporter exporter;
    private final SupabaseStorageService supabaseStorageService;

    @Value("${google.template.id}")
    private String templateId;

    // Elimina un conjunto de slides por ID
    public void removeSlides(String presentationId, List<String> slideIds) throws IOException {
        List<Request> requests = slideIds.stream()
                .map(id -> new Request().setDeleteObject(new DeleteObjectRequest().setObjectId(id)))
                .collect(Collectors.toList());

        authService.getSlidesService()
                .presentations()
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                .execute();
    }

    // Crea una nueva presentación a partir de la plantilla base
    public String copyPresentation(String nombre) throws IOException {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la presentación no puede estar vacío.");
        }

        var nueva = new com.google.api.services.drive.model.File();
        nueva.setName(nombre);
        return authService.getDriveService().files().copy(templateId, nueva).execute().getId();
    }

    // Reemplaza el texto <<trimester>> y <<year>> en la presentación
    public void replaceBasicData(String presentationId, String trimester, int year) throws IOException {
        if (presentationId == null || presentationId.isBlank()) {
            throw new IllegalArgumentException("El ID de la presentación no puede estar vacío.");
        }

        textEditor.replaceText(presentationId, Map.of(
                "<<trimester>>", trimester,
                "<<year>>", String.valueOf(year)
        ));
    }

    // Inserta una imagen del cronograma en la slide correspondiente
    public void insertScheduleImage(String presentationId, String slideId, String scheduleUrl) throws IOException {
        if (presentationId == null || slideId == null) {
            throw new IllegalArgumentException("El ID de la presentación y el ID de la diapositiva no pueden estar vacíos.");
        }
        if (scheduleUrl != null && !scheduleUrl.isBlank()) {
            imageEditor.insertImageFromUrl(presentationId, slideId, scheduleUrl, 130, 110, 700, 440);
        }
    }

    // Inserta secciones por cada taller: título, imágenes y tabla de asistencia si aplica
    public List<String> insertWorkshops(String presentationId, List<ReportWorkshopDto> talleres) throws IOException {
        if (presentationId == null || talleres == null) {
            throw new IllegalArgumentException("El ID de la presentación y la lista de talleres no pueden estar vacíos.");
        }

        List<String> orderedSlides = new ArrayList<>();

        for (ReportWorkshopDto taller : talleres) {
            // Slide de título y descripción
            String slideNombreId = "taller_" + UUID.randomUUID();
            authService.getSlidesService().presentations().batchUpdate(presentationId,
                    new BatchUpdatePresentationRequest().setRequests(List.of(
                            new Request().setDuplicateObject(
                                    new DuplicateObjectRequest()
                                            .setObjectId("p5")
                                            .setObjectIds(Map.of("p5", slideNombreId))
                            )
                    ))).execute();

            textEditor.insertTextInSlide(
                    presentationId,
                    slideNombreId,
                    taller.getWorkshopName(),
                    taller.getDescription(),
                    60, 180, 800, 100
            );

            orderedSlides.add(slideNombreId);

            // Slides de imágenes (una por cada una)
            if (taller.getImageUrl() != null && taller.getImageUrl().length > 0) {
                List<String> imageUrls = Arrays.asList(taller.getImageUrl());

                List<String> imageSlideIds = imageEditor.insertMultipleImages(
                        presentationId, "g312b8373c9a_0_1", imageUrls
                );

                orderedSlides.addAll(imageSlideIds);
            }

            // Slides de resumen de asistencia
            if (taller.getWorkshopId() != null &&
                    taller.getAttendanceSummaries() != null &&
                    !taller.getAttendanceSummaries().isEmpty()) {

                List<String> resumenSlideIds = tableEditor.insertTableAssistanceMultipage(
                        presentationId,
                        "p6",
                        taller.getAttendanceSummaries().stream().map(dto -> {
                            var model = new ReportAttendanceSummary();
                            model.setPersonName(dto.getPersonName());
                            model.setPresentCount(dto.getPresentCount());
                            model.setAbsentCount(dto.getAbsentCount());
                            model.setLateCount(dto.getLateCount());
                            model.setJustifiedCount(dto.getJustifiedCount());
                            return model;
                        }).toList()
                );

                orderedSlides.addAll(resumenSlideIds);
            }

            // Espera para evitar errores por concurrencia
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return orderedSlides;
    }

    // Exporta una presentación como PDF
    public ByteArrayOutputStream exportAsPDF(String presentationId) throws IOException {
        if (presentationId == null || presentationId.isBlank()) {
            throw new IllegalArgumentException("El ID de la presentación no puede estar vacío.");
        }

        return exporter.exportPDF(presentationId);
    }

    // Exporta una presentación como PPTX
    public ByteArrayOutputStream exportAsPPTX(String presentationId) throws IOException {
        if (presentationId == null || presentationId.isBlank()) {
            throw new IllegalArgumentException("El ID de la presentación no puede estar vacío.");
        }

        return exporter.exportPPTX(presentationId);
    }

    // Genera toda la presentación desde un DTO completo
    public String generatePresentationFromDto(ReportWithWorkshopsDto dto) throws IOException {
        if (dto == null) {
            throw new IllegalArgumentException("El DTO no puede estar vacío.");
        }

        String presentationId = copyPresentation("Reporte Trimestral " + dto.getReport().getId());

        replaceBasicData(presentationId, dto.getReport().getTrimester(), dto.getReport().getYear());

        htmlEditor.insertFromHtmlUrl(presentationId, "p2", dto.getReport().getDescriptionUrl(), 10d);

        List<String> orderedSlides = new ArrayList<>();
        orderedSlides.add("p1");
        orderedSlides.add("p2");

        // Agregar cronograma solo si es primer trimestre
        if (!"Enero-Marzo".equalsIgnoreCase(dto.getReport().getTrimester())) {
            removeSlides(presentationId, List.of("p3", "p4"));
        } else if (dto.getReport().getScheduleUrl() != null && !dto.getReport().getScheduleUrl().isBlank()) {
            insertScheduleImage(presentationId, "p4", dto.getReport().getScheduleUrl());
            orderedSlides.add("p3");
            orderedSlides.add("p4");
        }

        // Insertar talleres y agregarlos al orden
        List<String> tallerSlides = insertWorkshops(presentationId, dto.getWorkshops());
        orderedSlides.addAll(tallerSlides);

        // Reordenar slides según el orden final deseado
        List<Request> reorderRequests = new ArrayList<>();
        for (int i = 0; i < orderedSlides.size(); i++) {
            reorderRequests.add(new Request().setUpdateSlidesPosition(
                    new UpdateSlidesPositionRequest()
                            .setSlideObjectIds(List.of(orderedSlides.get(i)))
                            .setInsertionIndex(i)
            ));
        }

        authService.getSlidesService().presentations()
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(reorderRequests))
                .execute();

        // Eliminar slides base ya usados
        removeSlides(presentationId, List.of("p5", "g312b8373c9a_0_1", "p6"));

        return presentationId;
    }

    // Exporta el archivo en el formato indicado y devuelve una respuesta HTTP
    public Mono<ResponseEntity<byte[]>> exportFile(ReportWithWorkshopsDto dto, String tipo, Integer id,
                                                   LocalDate startDate, LocalDate endDate) {
        if (dto == null || tipo == null || id == null) {
            return Mono.error(new IllegalArgumentException("Los parámetros de entrada no pueden estar vacíos."));
        }

        // 1. Construir nombre del archivo
        String fechaSuffix = "";
        if (startDate != null && endDate != null) {
            fechaSuffix = "_del_" + startDate + "_al_" + endDate;
        } else if (startDate != null) {
            fechaSuffix = "_desde_" + startDate;
        } else if (endDate != null) {
            fechaSuffix = "_hasta_" + endDate;
        }

        String extension = tipo.toLowerCase();
        String filename = "report_" + id + fechaSuffix + "." + extension;
        String folder = extension.equals("pdf") ? "pdf" : "pptx";
        MediaType contentType = extension.equals("pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");

        // 2. Verificar si ya existe
        return supabaseStorageService.fileExists(folder, filename)
                .flatMap(exists -> {
                    if (exists) {
                        String publicUrl = supabaseStorageService.getPublicUrl(folder, filename);
                        return Mono.just(
                                ResponseEntity.status(302)  // Redirección a la URL existente
                                        .header(HttpHeaders.LOCATION, publicUrl)
                                        .build()
                        );
                    }

                    // 3. Generar presentación
                    return Mono.fromCallable(() -> generatePresentationFromDto(dto))
                            .flatMap(presentationId -> {
                                Mono<ByteArrayOutputStream> exportMono;

                                if (extension.equals("pdf")) {
                                    exportMono = Mono.fromCallable(() -> exportAsPDF(presentationId));
                                } else if (extension.equals("pptx")) {
                                    exportMono = Mono.fromCallable(() -> exportAsPPTX(presentationId));
                                } else {
                                    return Mono.error(new IllegalArgumentException("Tipo de archivo no soportado: " + tipo));
                                }

                                return exportMono
                                        .flatMap(output -> {
                                            byte[] bytes = output.toByteArray();
                                            return supabaseStorageService.uploadPdf(folder, filename, bytes)
                                                    .map(publicUrl -> ResponseEntity.ok()
                                                            .contentType(contentType)
                                                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                                                            .body(bytes));
                                        });
                            });
                });
    }
}
