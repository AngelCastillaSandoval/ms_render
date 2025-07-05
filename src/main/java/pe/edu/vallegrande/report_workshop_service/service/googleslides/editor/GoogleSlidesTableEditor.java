package pe.edu.vallegrande.report_workshop_service.service.googleslides.editor;

import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.report_workshop_service.model.ReportAttendanceSummary;
import pe.edu.vallegrande.report_workshop_service.service.googleslides.auth.GoogleAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inserta tablas de resumen de asistencia en slides.
 */
@Component
@RequiredArgsConstructor
public class GoogleSlidesTableEditor {

    private final GoogleAuthService authService;

    // Inserta una tabla dividida en varias slides si hay muchas filas
    public List<String> insertTableAssistanceMultipage(String presentationId, String slideBaseId,
                                                       List<ReportAttendanceSummary> resumen) throws IOException {
        Slides slidesService = authService.getSlidesService();
        List<String> generatedSlideIds = new ArrayList<>();

        int rowsPerPage = 10;
        int totalPages = (int) Math.ceil(resumen.size() / (double) rowsPerPage);

        for (int page = 0; page < totalPages; page++) {
            String newSlideId = "tabslide_" + UUID.randomUUID();
            String tableId = "table_" + UUID.randomUUID();

            // Duplicar slide base
            slidesService.presentations()
                    .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(List.of(
                            new Request().setDuplicateObject(new DuplicateObjectRequest()
                                    .setObjectId(slideBaseId)
                                    .setObjectIds(Map.of(slideBaseId, newSlideId)))
                    )))
                    .execute();

            // Crear tabla con tamaño y posición
            int rows = Math.min(rowsPerPage, resumen.size() - page * rowsPerPage) + 1; // +1 para encabezado
            List<Request> requests = new ArrayList<>();

            requests.add(new Request().setCreateTable(new CreateTableRequest()
                    .setObjectId(tableId)
                    .setElementProperties(new PageElementProperties()
                            .setPageObjectId(newSlideId)
                            .setSize(new Size()
                                    .setHeight(new Dimension().setMagnitude(300d).setUnit("PT"))
                                    .setWidth(new Dimension().setMagnitude(700d).setUnit("PT")))
                            .setTransform(new AffineTransform()
                                    .setScaleX(1d).setScaleY(1d)
                                    .setTranslateX(150d).setTranslateY(90d)
                                    .setUnit("PT")))
                    .setRows(rows)
                    .setColumns(5)));

            // Ajustar ancho de columnas
            double[] columnWidths = {240, 90, 90, 90, 90};
            for (int col = 0; col < columnWidths.length; col++) {
                requests.add(new Request().setUpdateTableColumnProperties(
                        new UpdateTableColumnPropertiesRequest()
                                .setObjectId(tableId)
                                .setColumnIndices(List.of(col))
                                .setTableColumnProperties(new TableColumnProperties()
                                        .setColumnWidth(new Dimension()
                                                .setMagnitude(columnWidths[col])
                                                .setUnit("PT")))
                                .setFields("columnWidth")));
            }

            // Insertar encabezados
            String[] headers = {"Nombre", "Asistencias", "Faltas", "Tardanzas", "Justificados"};
            for (int col = 0; col < headers.length; col++) {
                requests.addAll(insertCellText(tableId, 0, col, headers[col], true));
            }

            // Insertar filas con datos
            for (int i = 0; i < rows - 1; i++) {
                ReportAttendanceSummary r = resumen.get(page * rowsPerPage + i);
                requests.addAll(insertCellText(tableId, i + 1, 0, r.getPersonName(), false));
                requests.addAll(insertCellText(tableId, i + 1, 1, String.valueOf(r.getPresentCount()), false));
                requests.addAll(insertCellText(tableId, i + 1, 2, String.valueOf(r.getAbsentCount()), false));
                requests.addAll(insertCellText(tableId, i + 1, 3, String.valueOf(r.getLateCount()), false));
                requests.addAll(insertCellText(tableId, i + 1, 4, String.valueOf(r.getJustifiedCount()), false));
            }

            // Ejecutar requests para crear la tabla
            slidesService.presentations()
                    .batchUpdate(presentationId, new BatchUpdatePresentationRequest().setRequests(requests))
                    .execute();

            generatedSlideIds.add(newSlideId);
        }

        return generatedSlideIds;
    }

    // Crea una celda con texto, estilo y alineación centrada
    private List<Request> insertCellText(String tableId, int row, int column, String text, boolean bold) {
        List<Request> requests = new ArrayList<>();

        requests.add(new Request().setInsertText(new InsertTextRequest()
                .setObjectId(tableId)
                .setCellLocation(new TableCellLocation().setRowIndex(row).setColumnIndex(column))
                .setText(text)));

        TextStyle style = new TextStyle()
                .setBold(bold)
                .setFontSize(new Dimension().setMagnitude(12d).setUnit("PT"));

        requests.add(new Request().setUpdateTextStyle(new UpdateTextStyleRequest()
                .setObjectId(tableId)
                .setCellLocation(new TableCellLocation().setRowIndex(row).setColumnIndex(column))
                .setTextRange(new Range().setType("ALL"))
                .setStyle(style)
                .setFields("bold,fontSize")));

        requests.add(new Request().setUpdateTableCellProperties(new UpdateTableCellPropertiesRequest()
                .setObjectId(tableId)
                .setTableRange(new TableRange()
                        .setLocation(new TableCellLocation().setRowIndex(row).setColumnIndex(column))
                        .setRowSpan(1).setColumnSpan(1))
                .setTableCellProperties(new TableCellProperties().setContentAlignment("MIDDLE"))
                .setFields("contentAlignment")));

        return requests;
    }
}
