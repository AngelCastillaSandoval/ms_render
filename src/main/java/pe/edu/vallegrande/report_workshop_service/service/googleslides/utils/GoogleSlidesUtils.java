package pe.edu.vallegrande.report_workshop_service.service.googleslides.utils;

import com.google.api.services.slides.v1.model.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GoogleSlidesUtils {

    /**
     * Convierte un color HTML (#rrggbb o rgb(...)) a un arreglo de RGB en String.
     */
    public static String[] parseColor(String color) {
        if (color == null || color.isBlank()) return null;

        if (color.startsWith("#")) {
            try {
                int r = Integer.parseInt(color.substring(1, 3), 16);
                int g = Integer.parseInt(color.substring(3, 5), 16);
                int b = Integer.parseInt(color.substring(5, 7), 16);
                return new String[]{String.valueOf(r), String.valueOf(g), String.valueOf(b)};
            } catch (Exception e) {
                return null;
            }
        }

        if (color.startsWith("rgb(")) {
            try {
                String[] values = color.replace("rgb(", "").replace(")", "").split(",");
                return new String[]{
                        values[0].trim(),
                        values[1].trim(),
                        values[2].trim()
                };
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Crea un cuadro de texto con formato desde HTML (negrita, cursiva, color, etc.).
     */
    public static List<Request> createFormattedText(String slideId, String text, double posY, Element htmlElement) {
        String objectId = "desc_" + UUID.randomUUID(); // ID único
        List<Request> requests = new ArrayList<>();

        // 1. Crear cuadro de texto
        requests.add(new Request().setCreateShape(new CreateShapeRequest()
                .setObjectId(objectId)
                .setShapeType("TEXT_BOX")
                .setElementProperties(new PageElementProperties()
                        .setPageObjectId(slideId)
                        .setSize(new Size()
                                .setHeight(new Dimension().setMagnitude(300d).setUnit("PT"))
                                .setWidth(new Dimension().setMagnitude(840d).setUnit("PT")))
                        .setTransform(new AffineTransform()
                                .setScaleX(1.0)
                                .setScaleY(1.0)
                                .setTranslateX(40.0)
                                .setTranslateY(posY)
                                .setUnit("PT")))));

        // 2. Insertar texto
        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setObjectId(objectId)
                .setInsertionIndex(0)
                .setText(text)));

        // 3. Construir estilo de texto (si aplica)
        TextStyle style = new TextStyle();
        StringBuilder fields = new StringBuilder();

        if (htmlElement.select("b, strong").size() > 0) {
            style.setBold(true);
            fields.append("bold,");
        }
        if (htmlElement.select("i, em").size() > 0) {
            style.setItalic(true);
            fields.append("italic,");
        }
        if (htmlElement.select("u").size() > 0) {
            style.setUnderline(true);
            fields.append("underline,");
        }
        if (htmlElement.select("s, strike").size() > 0) {
            style.setStrikethrough(true);
            fields.append("strikethrough,");
        }

        // Font size
        String fontSize = htmlElement.attr("style").replaceAll(".*font-size:\\s*(\\d+)pt.*", "$1");
        if (fontSize.matches("\\d+")) {
            style.setFontSize(new Dimension().setMagnitude(Double.parseDouble(fontSize)).setUnit("PT"));
            fields.append("fontSize,");
        }

        // Color
        String color = htmlElement.attr("style").replaceAll(".*color:\\s*([^;]+);?.*", "$1");
        if (!color.isBlank()) {
            String[] rgb = parseColor(color);
            if (rgb != null) {
                RgbColor rgbColor = new RgbColor()
                        .setRed(Float.parseFloat(rgb[0]) / 255)
                        .setGreen(Float.parseFloat(rgb[1]) / 255)
                        .setBlue(Float.parseFloat(rgb[2]) / 255);
                style.setForegroundColor(new OptionalColor().setOpaqueColor(
                        new OpaqueColor().setRgbColor(rgbColor)
                ));
                fields.append("foregroundColor,");
            }
        }

        // Agregar estilo de texto si se definió algo
        if (fields.length() > 0) {
            String cleanFields = fields.toString().replaceAll(",$", "");
            requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
                    .setObjectId(objectId)
                    .setTextRange(new Range().setType("ALL"))
                    .setStyle(style)
                    .setFields(cleanFields)));
        }

        // 4. Alineación (solo en Request separado)
        String align = htmlElement.attr("style").replaceAll(".*text-align:\\s*(\\w+).*", "$1");
        if (!align.isBlank()) {
            String alignValue = align.toUpperCase();
            if (List.of("JUSTIFIED", "CENTER", "RIGHT", "START").contains(alignValue)) {
                ParagraphStyle paragraphStyle = new ParagraphStyle().setAlignment(alignValue);
                requests.add(new Request().setUpdateParagraphStyle(new UpdateParagraphStyleRequest()
                        .setObjectId(objectId)
                        .setTextRange(new Range().setType("ALL"))
                        .setStyle(paragraphStyle)
                        .setFields("alignment")));
            }
        }

        return requests;
    }

    /**
     * Crea un cuadro de texto simple (sin formato HTML), con estilo personalizado.
     */
    public static List<Request> createSimpleTextBox(String objectId, String slideId, String texto,
                                                    double x, double y, double width, double height,
                                                    String fontFamily, int fontSize, String alignment) {
        List<Request> requests = new ArrayList<>();

        // Validar que el texto no sea null ni vacío
        if (texto == null || texto.trim().isEmpty()) {
            return requests; // Retornar lista vacía si no hay texto
        }

        // Crear cuadro de texto
        requests.add(new Request().setCreateShape(new CreateShapeRequest()
                .setObjectId(objectId)
                .setShapeType("TEXT_BOX")
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

        // Insertar texto
        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setObjectId(objectId)
                .setInsertionIndex(0)
                .setText(texto.trim()))); // Usar trim() para limpiar espacios

        // Estilo de texto
        TextStyle style = new TextStyle()
                .setFontFamily(fontFamily)
                .setFontSize(new Dimension().setMagnitude((double) fontSize).setUnit("PT"));

        requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
                .setObjectId(objectId)
                .setTextRange(new Range().setType("ALL"))
                .setStyle(style)
                .setFields("fontFamily,fontSize")));

        // Alineación si se indicó
        if (alignment != null && !alignment.trim().isEmpty()) {
            requests.add(new Request().setUpdateParagraphStyle(new UpdateParagraphStyleRequest()
                    .setObjectId(objectId)
                    .setTextRange(new Range().setType("ALL"))
                    .setStyle(new ParagraphStyle().setAlignment(alignment))
                    .setFields("alignment")));
        }

        return requests;
    }
}
