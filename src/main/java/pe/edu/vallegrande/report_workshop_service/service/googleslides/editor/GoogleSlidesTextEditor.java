package pe.edu.vallegrande.report_workshop_service.service.googleslides.editor;

import com.google.api.services.slides.v1.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.utils.GoogleSlidesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inserta y reemplaza texto en presentaciones de Google Slides.
 */
@Component
@RequiredArgsConstructor
public class GoogleSlidesTextEditor {

    private final GoogleAuthService authService;

    // Reemplaza textos como <<year>> o <<trimester>> en todas las slides
    public void replaceText(String presentationId, Map<String, String> data) throws IOException {
        List<Request> requests = data.entrySet().stream()
                .map(entry -> new Request().setReplaceAllText(new ReplaceAllTextRequest()
                        .setContainsText(new SubstringMatchCriteria()
                                .setText(entry.getKey())
                                .setMatchCase(true))
                        .setReplaceText(entry.getValue())))
                .toList();

        authService.getSlidesService()
                .presentations()
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                .execute();
    }

    // Inserta un título y una descripción en una slide
    public void insertTextInSlide(String presentationId, String slideId, String titulo, String descripcion,
                                  double x, double y, double width, double height) throws IOException {

        List<Request> requests = new ArrayList<>();

        // Título centrado con fuente grande
        if (titulo != null && !titulo.trim().isEmpty()) {
            String tituloId = "titulo_" + UUID.randomUUID();
            requests.addAll(GoogleSlidesUtils.createSimpleTextBox(
                    tituloId, slideId, titulo, x, y, width, height,
                    "Abril Fatface", 44, "CENTER"
            ));
        }

        // Descripción debajo del título - SOLO si no es null ni vacía
        if (descripcion != null && !descripcion.trim().isEmpty()) {
            String descripcionId = "desc_" + UUID.randomUUID();
            requests.addAll(GoogleSlidesUtils.createSimpleTextBox(
                    descripcionId, slideId, descripcion,
                    x, y + height + 20, width, 200,
                    "Arial", 16, null
            ));
        }

        // Enviar requests solo si hay alguna
        if (!requests.isEmpty()) {
            authService.getSlidesService()
                    .presentations()
                    .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                    .execute();
        }
    }
}
