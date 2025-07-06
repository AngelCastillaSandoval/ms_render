package pe.edu.vallegrande.report_workshop_service.service.googleslides.editor;

import com.google.api.services.slides.v1.model.BatchUpdatePresentationRequest;
import com.google.api.services.slides.v1.model.Request;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.utils.GoogleSlidesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserta contenido HTML en diapositivas de Google Slides.
 */
@Component
@RequiredArgsConstructor
public class GoogleSlidesHtmlEditor {

    private final GoogleAuthService authService;

    // Inserta texto HTML directo en una slide
    public void insertFromHtml(String presentationId, String slideId, String html, double startY) throws IOException {
        if (html == null || html.trim().isEmpty()) {
            return; // No hacer nada si el HTML es null o vacío
        }

        List<Request> requests = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        StringBuilder textoCompleto = new StringBuilder();
        for (Element element : doc.body().children()) {
            if (element.tagName().equals("ul") || element.tagName().equals("ol")) {
                boolean isOrdered = element.tagName().equals("ol");
                int olCounter = 1;
                for (Element li : element.children()) {
                    String prefix = isOrdered ? (olCounter++) + ". " : "• ";
                    textoCompleto.append(prefix).append(li.text()).append("\n");
                }
            } else if (element.tagName().equals("p")) {
                if (!element.text().isBlank()) {
                    textoCompleto.append(element.text()).append("\n");
                }
            }
        }

        // Validar que hay texto para insertar
        if (textoCompleto.toString().trim().isEmpty()) {
            return; // No crear cuadro si no hay texto
        }

        requests.addAll(GoogleSlidesUtils.createFormattedText(slideId, textoCompleto.toString(), startY, doc.body()));

        if (!requests.isEmpty()) {
            authService.getSlidesService()
                    .presentations()
                    .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                    .execute();
        }
    }

    // Inserta HTML desde una URL remota
    public void insertFromHtmlUrl(String presentationId, String slideId, String htmlUrl, double startY) throws IOException {
        String html = Jsoup.connect(htmlUrl).get().html();
        insertFromHtml(presentationId, slideId, html, startY);
    }
}
