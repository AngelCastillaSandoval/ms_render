package pe.edu.vallegrande.report_workshop_service.service.googleslides.exporter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Exporta presentaciones de Google Slides a PDF o PPTX.
 */
@Component
@RequiredArgsConstructor
public class GoogleSlidesExporter {

    private final GoogleAuthService authService;

    /**
     * Exporta la presentación como PDF.
     * Id del archivo en Drive
     * Archivo PDF como flujo de bytes
     */
    public ByteArrayOutputStream exportPDF(String fileId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            authService.getDriveService()
                    .files()
                    .export(fileId, "application/pdf")
                    .executeMediaAndDownloadTo(out);
        } catch (IOException e) {
            // Manejo de excepciones adecuado
            throw new RuntimeException("Error al exportar la presentación como PDF", e);
        }
        return out;
    }

    /**
     * Exporta la presentación como PPTX.
     * Id del archivo en Drive
     * Archivo PPTX como flujo de bytes
     */
    public ByteArrayOutputStream exportPPTX(String fileId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            authService.getDriveService()
                    .files()
                    .export(fileId, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    .executeMediaAndDownloadTo(out);
        } catch (IOException e) {
            // Manejo de excepciones adecuado
            throw new RuntimeException("Error al exportar la presentación como PPTX", e);
        }
        return out;
    }
}
