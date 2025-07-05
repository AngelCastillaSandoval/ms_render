package pe.edu.vallegrande.report_workshop_service.service.googleslides.editor;

import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inserta imágenes en presentaciones de Google Slides.
 */
@Component
@RequiredArgsConstructor
public class GoogleSlidesImageEditor {

    private final GoogleAuthService authService;

    // Inserta una imagen en una slide
    public void insertImageFromUrl(String presentationId, String slideId, String imageUrl,
                                   double x, double y, double width, double height) throws IOException {
        String imageId = "img_" + UUID.randomUUID();

        List<Request> requests = new ArrayList<>();

        requests.add(new Request().setCreateImage(new CreateImageRequest()
                .setObjectId(imageId)
                .setUrl(imageUrl)
                .setElementProperties(new PageElementProperties()
                        .setPageObjectId(slideId)
                        .setSize(new Size()
                                .setHeight(new Dimension().setMagnitude(height).setUnit("PT"))
                                .setWidth(new Dimension().setMagnitude(width).setUnit("PT")))
                        .setTransform(new AffineTransform()
                                .setScaleX(1.0)
                                .setScaleY(1.0)
                                .setTranslateX(x)
                                .setTranslateY(y)
                                .setUnit("PT")))));

        authService.getSlidesService()
                .presentations()
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                .execute();
    }

    // Inserta varias imágenes en nuevas slides duplicadas
    public List<String> insertMultipleImages(String presentationId, String slideBaseId, List<String> imageUrls)
            throws IOException {
        List<Request> requests = new ArrayList<>();
        List<String> slideIds = new ArrayList<>();
        Slides slidesService = authService.getSlidesService();

        double slideWidth = 960d;
        double slideHeight = 510d;
        double imageWidth = 800d;
        double imageHeight = 700d;
        double x = (slideWidth - imageWidth) / 2;
        double y = (slideHeight - imageHeight) / 2;

        for (String imageUrl : imageUrls) {
            String newSlideId = "imgslide_" + UUID.randomUUID();
            String imageId = "img_" + UUID.randomUUID();

            slideIds.add(newSlideId);

            requests.add(new Request().setDuplicateObject(new DuplicateObjectRequest()
                    .setObjectId(slideBaseId)
                    .setObjectIds(Map.of(slideBaseId, newSlideId))));

            requests.add(new Request().setCreateImage(new CreateImageRequest()
                    .setObjectId(imageId)
                    .setUrl(imageUrl)
                    .setElementProperties(new PageElementProperties()
                            .setPageObjectId(newSlideId)
                            .setSize(new Size()
                                    .setHeight(new Dimension().setMagnitude(imageHeight).setUnit("PT"))
                                    .setWidth(new Dimension().setMagnitude(imageWidth).setUnit("PT")))
                            .setTransform(new AffineTransform()
                                    .setScaleX(1.0)
                                    .setScaleY(1.0)
                                    .setTranslateX(x)
                                    .setTranslateY(y)
                                    .setUnit("PT")))));
        }

        slidesService.presentations()
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                .execute();

        return slideIds;
    }
}
