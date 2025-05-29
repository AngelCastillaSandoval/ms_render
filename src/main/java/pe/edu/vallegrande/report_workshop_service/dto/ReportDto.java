package pe.edu.vallegrande.report_workshop_service.dto;

import lombok.Data;

@Data
public class ReportDto {
    private Integer id;
    private Integer year;
    private String trimester;
    private String descriptionUrl;
    private String scheduleUrl;
    private String status;
}
