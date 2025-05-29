package pe.edu.vallegrande.report_workshop_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.dto.*;
import pe.edu.vallegrande.report_workshop_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_workshop_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_workshop_service.repository.WorkshopCacheRepository;
import pe.edu.vallegrande.report_workshop_service.webclient.ReportCoreClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportWorkshopService {

    private final ReportCoreClient reportClient;
    private final ReportWorkshopRepository reportWorkshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;
    private final SupabaseStorageService storageService;

    private int getTrimesterOrder(String trimester) {
        return switch (trimester.toLowerCase()) {
            case "enero-marzo" -> 1;
            case "abril-junio" -> 2;
            case "julio-septiembre" -> 3;
            case "octubre-diciembre" -> 4;
            default -> 5;
        };
    }

    public Flux<ReportWithWorkshopsDto> findFilteredReports(String status, String trimester, Integer year, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        return reportClient.findAll()
                .filter(r -> status == null || status.equalsIgnoreCase(r.getStatus()))
                .filter(r -> trimester == null || trimester.equalsIgnoreCase(r.getTrimester()))
                .filter(r -> year == null || year.equals(r.getYear()))
                .flatMap(report -> reportWorkshopRepo.findByReportId(report.getId())
                        .flatMap(rw -> buildDtoWithDateFilter(rw, workshopDateStart, workshopDateEnd))
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(report);
                            dto.setWorkshops(workshops);
                            return dto;
                        })
                )
                .sort(Comparator.comparing((ReportWithWorkshopsDto r) -> r.getReport().getYear()).reversed()
                        .thenComparing(r -> getTrimesterOrder(r.getReport().getTrimester())));
    }

    public Mono<ReportWithWorkshopsDto> findByIdWithDateFilter(Integer id, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        return reportClient.findById(id)
                .flatMap(report -> reportWorkshopRepo.findByReportId(id)
                        .flatMap(rw -> buildDtoWithDateFilter(rw, workshopDateStart, workshopDateEnd))
                        .collectList()
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(report);
                            dto.setWorkshops(workshops);
                            return dto;
                        }));
    }

    private Mono<ReportWorkshopDto> buildDtoWithDateFilter(ReportWorkshop rw, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        ReportWorkshopDto dto = toDto(rw);

        if (rw.getWorkshopId() != null) {
            return workshopCacheRepo.findById(rw.getWorkshopId())
                    .filter(wc -> {
                        boolean inRange = true;
                        if (workshopDateStart != null) inRange = !wc.getDateStart().isBefore(workshopDateStart);
                        if (workshopDateEnd != null) inRange = inRange && !wc.getDateEnd().isAfter(workshopDateEnd);
                        return inRange;
                    })
                    .map(wc -> {
                        dto.setWorkshopStatus(wc.getStatus());
                        dto.setWorkshopDateStart(wc.getDateStart());
                        dto.setWorkshopDateEnd(wc.getDateEnd());
                        dto.setWorkshopName(wc.getName());
                        return dto;
                    });
        } else {
            boolean inRange = true;
            if (workshopDateStart != null && rw.getWorkshopDateStart() != null) {
                inRange = !rw.getWorkshopDateStart().isBefore(workshopDateStart);
            }
            if (workshopDateEnd != null && rw.getWorkshopDateEnd() != null) {
                inRange = inRange && !rw.getWorkshopDateEnd().isAfter(workshopDateEnd);
            }
            return inRange ? Mono.just(dto) : Mono.empty();
        }
    }

    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        return reportClient.create(dto.getReport())
                .flatMap(savedReport -> Flux.fromIterable(dto.getWorkshops())
                        .flatMap(workshopDto -> {
                            ReportWorkshop rw = fromDto(workshopDto);
                            rw.setReportId(savedReport.getId());

                            if (rw.getWorkshopId() != null) {
                                return workshopCacheRepo.findById(rw.getWorkshopId())
                                        .map(cache -> {
                                            rw.setWorkshopName(cache.getName());
                                            rw.setWorkshopDateStart(cache.getDateStart());
                                            rw.setWorkshopDateEnd(cache.getDateEnd());
                                            return rw;
                                        });
                            }
                            return Mono.just(rw);
                        })
                        .collectList()
                        .flatMapMany(reportWorkshopRepo::saveAll)
                        .collectList()
                        .map(savedWorkshops -> {
                            ReportWithWorkshopsDto result = new ReportWithWorkshopsDto();
                            result.setReport(savedReport);
                            result.setWorkshops(savedWorkshops.stream().map(this::toDto).toList());
                            return result;
                        })
                );
    }

    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportClient.update(id, dto.getReport())
                .flatMap(updatedReport -> reportWorkshopRepo.deleteByReportId(id)
                        .thenMany(Flux.fromIterable(dto.getWorkshops()))
                        .flatMap(workshopDto -> {
                            ReportWorkshop rw = fromDto(workshopDto);
                            rw.setReportId(id);

                            if (rw.getWorkshopId() != null) {
                                return workshopCacheRepo.findById(rw.getWorkshopId())
                                        .map(cache -> {
                                            rw.setWorkshopName(cache.getName());
                                            rw.setWorkshopDateStart(cache.getDateStart());
                                            rw.setWorkshopDateEnd(cache.getDateEnd());
                                            return rw;
                                        });
                            }
                            return Mono.just(rw);
                        })
                        .collectList()
                        .flatMapMany(reportWorkshopRepo::saveAll)
                        .collectList()
                        .map(savedWorkshops -> {
                            ReportWithWorkshopsDto result = new ReportWithWorkshopsDto();
                            result.setReport(updatedReport);
                            result.setWorkshops(savedWorkshops.stream().map(this::toDto).toList());
                            return result;
                        })
                );
    }

    public Mono<Void> disable(Integer id) {
        return reportClient.disable(id);
    }

    public Mono<Void> restore(Integer id) {
        return reportClient.restore(id);
    }

    public Mono<Void> delete(Integer id) {
        return reportClient.delete(id)
                .then(reportWorkshopRepo.deleteByReportId(id));
    }

    /**
     * 🔹 Generación de PDF de reporte por ID con filtro de fechas
     */
    public Mono<ResponseEntity<byte[]>> generatePdfByIdWithDateFilter(Integer reportId, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        String folder = "pdf";
        StringBuilder fileNameBuilder = new StringBuilder("reporte_" + reportId);
        if (workshopDateStart != null) {
            fileNameBuilder.append("_from_").append(workshopDateStart);
        }
        if (workshopDateEnd != null) {
            fileNameBuilder.append("_to_").append(workshopDateEnd);
        }
        fileNameBuilder.append(".pdf");

        String fileName = fileNameBuilder.toString();

        return storageService.fileExists(folder, fileName)
                .flatMap(exists -> {
                    if (exists) {
                        String url = storageService.getPublicUrl(folder, fileName);
                        HttpHeaders headers = new HttpHeaders();
                        headers.setLocation(URI.create(url));
                        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                                .headers(headers)
                                .body(new byte[0]));
                    }

                    return reportClient.findById(reportId)
                            .flatMap(report -> reportWorkshopRepo.findByReportId(reportId)
                                    .filter(rw -> {
                                        boolean inRange = true;
                                        if (workshopDateStart != null && rw.getWorkshopDateStart() != null) {
                                            inRange = !rw.getWorkshopDateStart().isBefore(workshopDateStart);
                                        }
                                        if (workshopDateEnd != null && rw.getWorkshopDateEnd() != null) {
                                            inRange = inRange && !rw.getWorkshopDateEnd().isAfter(workshopDateEnd);
                                        }
                                        return inRange;
                                    })
                                    .collectList()
                                    .flatMap(filteredWorkshops -> {
                                        try {
                                            InputStream inputStream = new ClassPathResource("reportPDF.jasper").getInputStream();
                                            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(inputStream);

                                            List<ReportPDFDto> reportData = new ArrayList<>();
                                            for (ReportWorkshop workshop : filteredWorkshops) {
                                                ReportPDFDto dto = new ReportPDFDto();
                                                dto.setReport_id(report.getId());
                                                dto.setReport_year(report.getYear());
                                                dto.setTrimester(report.getTrimester());
                                                dto.setReport_description(report.getDescriptionUrl()); // Es la URL
                                                dto.setSchedule(report.getScheduleUrl());
                                                dto.setStatus(report.getStatus());
                                                dto.setWorkshop_id(workshop.getId());
                                                dto.setWorkshop_name(workshop.getWorkshopName());
                                                dto.setWorkshop_description(workshop.getDescription());
                                                dto.setImage_url(workshop.getImageUrl());
                                                reportData.add(dto);
                                            }

                                            // 🔁 Leer el HTML desde la URL
                                            String htmlContent = "";
                                            try (InputStream htmlStream = new URL(report.getDescriptionUrl()).openStream()) {
                                                htmlContent = new String(htmlStream.readAllBytes(), StandardCharsets.UTF_8);
                                            } catch (Exception ex) {
                                                log.warn("⚠️ No se pudo leer el HTML desde la URL: {}", report.getDescriptionUrl(), ex);
                                            }

                                            // 🔧 Llenar parámetros
                                            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
                                            Map<String, Object> parameters = new HashMap<>();
                                            parameters.put("ReportTitle", "Reporte de Actividades");
                                            parameters.put("SUBREPORT_DIR", "images/");
                                            parameters.put("report_description_html_content", htmlContent);

                                            // 📄 Generar PDF
                                            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
                                            byte[] pdfBytes = baos.toByteArray();

                                            // ☁️ Subir a Supabase
                                            storageService.uploadPdf(folder, fileName, pdfBytes).subscribe();

                                            HttpHeaders headers = new HttpHeaders();
                                            headers.setContentType(MediaType.APPLICATION_PDF);
                                            headers.setContentDispositionFormData("attachment", fileName);
                                            return Mono.just(new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK));
                                        } catch (Exception e) {
                                            log.error("❌ Error al generar PDF:", e);
                                            return Mono.error(new RuntimeException("Error generando el PDF", e));
                                        }
                                    }))
                            .switchIfEmpty(Mono.error(new NoSuchElementException("Reporte no encontrado con ID: " + reportId)));
                });
    }

    private ReportWorkshopDto toDto(ReportWorkshop rw) {
        ReportWorkshopDto dto = new ReportWorkshopDto();
        dto.setId(rw.getId());
        dto.setReportId(rw.getReportId());
        dto.setWorkshopId(rw.getWorkshopId());
        dto.setWorkshopName(rw.getWorkshopName());
        dto.setWorkshopDateStart(rw.getWorkshopDateStart());
        dto.setWorkshopDateEnd(rw.getWorkshopDateEnd());
        dto.setDescription(rw.getDescription());
        dto.setImageUrl(rw.getImageUrl());
        return dto;
    }

    private ReportWorkshop fromDto(ReportWorkshopDto dto) {
        return ReportWorkshop.builder()
                .id(dto.getId())
                .reportId(dto.getReportId())
                .workshopId(dto.getWorkshopId())
                .workshopName(dto.getWorkshopName())
                .workshopDateStart(dto.getWorkshopDateStart())
                .workshopDateEnd(dto.getWorkshopDateEnd())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .build();
    }
}
