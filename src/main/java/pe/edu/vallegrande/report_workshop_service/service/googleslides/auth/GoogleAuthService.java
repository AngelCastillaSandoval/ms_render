package pe.edu.vallegrande.report_workshop_service.service.googleslides.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.SlidesScopes;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;

/**
 * Servicio de autenticación con Google Slides y Google Drive.
 * Usa credenciales en base64 y crea clientes autenticados para Google APIs.
 */
@Slf4j
@Component
public class GoogleAuthService {

    // 🔹 Inyectamos desde application.yml el valor de la variable base64
    @Value("${google.credentials.base64}")
    private String credentialsBase64;

    private Slides slidesService;
    private Drive driveService;

    /**
     * Inicializa los clientes Google al iniciar la app
     */
    @PostConstruct
    public void init() {
        try {
            var credential = GoogleCredential.fromStream(getCredentialsInputStream())
                    .createScoped(List.of(SlidesScopes.PRESENTATIONS, DriveScopes.DRIVE));

            slidesService = new Slides.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("Google Slides API").build();

            driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("Google Drive API").build();

            log.info("Google Slides y Drive inicializados correctamente");

        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al inicializar Google Services", e);
        }
    }

    public Slides getSlidesService() {
        return slidesService;
    }

    public Drive getDriveService() {
        return driveService;
    }

    /**
     * Decodifica el base64 y crea un InputStream temporal del archivo JSON
     */
    private FileInputStream getCredentialsInputStream() throws IOException {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            throw new IllegalStateException("❌ GOOGLE_CREDENTIALS_BASE64 no está configurada");
        }

        byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
        File tempFile = File.createTempFile("google-creds", ".json");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(decoded);
        }

        return new FileInputStream(tempFile);
    }
}
