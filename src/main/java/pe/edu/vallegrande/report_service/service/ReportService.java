package pe.edu.vallegrande.report_service.service;

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
import org.springframework.web.reactive.function.BodyInserters;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.dto.ReportPDFDto;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepo;
    private final ReportWorkshopRepository workshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;
    private final SupabaseStorageService storageService;

    /**
     * 🔹 Obtener reportes con talleres filtrados por fecha + otros filtros
     */
    public Flux<ReportWithWorkshopsDto> findFilteredReports(String status, String trimester, Integer year, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        Flux<Report> baseQuery = (status != null) ? reportRepo.findByStatus(status) : reportRepo.findAll();

        if (trimester != null) {
            baseQuery = baseQuery.filter(r -> trimester.equalsIgnoreCase(r.getTrimester()));
        }
        if (year != null) {
            baseQuery = baseQuery.filter(r -> year.equals(r.getYear()));
        }

        return baseQuery.flatMap(report ->
                workshopRepo.findByReportId(report.getId())
                        .flatMap(rw -> {
                            ReportWorkshopDto dto = toWorkshopDto(rw);

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
                        })
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(toDto(report));
                            dto.setWorkshops(workshops);
                            return dto;
                        })
        );
    }


    /**
     * 🔹 Obtener por ID con talleres filtrados por fechas
     */
    public Mono<ReportWithWorkshopsDto> findByIdWithDateFilter(Integer id, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        Mono<Report> reportMono = reportRepo.findById(id);

        Flux<ReportWorkshopDto> workshopsFlux = workshopRepo.findByReportId(id)
                .flatMap(rw -> {
                    ReportWorkshopDto dto = toWorkshopDto(rw);

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
                });

        return Mono.zip(reportMono, workshopsFlux.collectList(), (report, workshops) -> {
            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
            dto.setReport(toDto(report));
            dto.setWorkshops(workshops);
            return dto;
        });
    }


    /**
     * 🔹 Crear reporte con talleres (con soporte para talleres personalizados o reales)
     */
    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        Report report = fromDto(dto.getReport());
        report.setStatus("A");

        String rawSchedule = dto.getReport().getSchedule();
        Mono<String> scheduleMono = (rawSchedule == null)
                ? Mono.just("")
                : isBase64(rawSchedule)
                ? storageService.uploadBase64Image("reports/schedules", rawSchedule)
                : Mono.just(rawSchedule);

        return scheduleMono.flatMap(scheduleUrl -> {
            report.setSchedule(scheduleUrl);
            return reportRepo.save(report)
                    .flatMap(saved -> Flux.fromIterable(dto.getWorkshops())
                            .flatMap(dtoW -> Flux.fromArray(dtoW.getImageUrl())
                                    .flatMap(image -> isBase64(image)
                                            ? storageService.uploadBase64Image("reports/workshops", image)
                                            : Mono.just(image))
                                    .collectList()
                                    .flatMap(images -> {
                                        ReportWorkshop rw = fromWorkshopDto(dtoW);
                                        rw.setReportId(saved.getId());
                                        rw.setImageUrl(images.toArray(new String[0]));

                                        if (rw.getWorkshopId() != null) {
                                            return workshopCacheRepo.findById(rw.getWorkshopId())
                                                    .map(cache -> {
                                                        rw.setWorkshopName(cache.getName());
                                                        rw.setWorkshopDateStart(cache.getDateStart());
                                                        rw.setWorkshopDateEnd(cache.getDateEnd());
                                                        return rw;
                                                    });
                                        } else {
                                            return Mono.just(rw);
                                        }
                                    }))
                            .collectList()
                            .flatMap(workshops -> workshopRepo.saveAll(workshops).collectList())
                            .map(savedWorkshops -> {
                                ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                response.setReport(toDto(report));
                                response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                log.info("✅ Reporte creado: {}", response);
                                return response;
                            }));
        });
    }

    /**
     * 🔹 Editar reporte y reemplazar talleres (con manejo de imágenes en Supabase)
     */
    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportRepo.findById(id)
                .flatMap(existing -> {
                    existing.setYear(dto.getReport().getYear());
                    existing.setTrimester(dto.getReport().getTrimester());
                    existing.setDescription(dto.getReport().getDescription());

                    String rawSchedule = dto.getReport().getSchedule();
                    Mono<String> scheduleMono = rawSchedule == null
                            ? Mono.just(existing.getSchedule())
                            : isBase64(rawSchedule)
                            ? storageService.uploadBase64Image("reports/schedules", rawSchedule)
                            : Mono.just(rawSchedule);

                    return scheduleMono.flatMap(scheduleUrl -> {
                        existing.setSchedule(scheduleUrl);

                        return workshopRepo.findByReportId(id)
                                .collectList()
                                .flatMap(oldWorkshops -> {
                                    List<String> oldUrls = oldWorkshops.stream()
                                            .flatMap(rw -> rw.getImageUrl() == null ? Stream.empty() : Arrays.stream(rw.getImageUrl()))
                                            .collect(Collectors.toList());

                                    List<String> newUrls = dto.getWorkshops().stream()
                                            .flatMap(w -> w.getImageUrl() == null ? Stream.empty() : Arrays.stream(w.getImageUrl()))
                                            .collect(Collectors.toList());

                                    List<String> toDelete = oldUrls.stream()
                                            .filter(url -> !newUrls.contains(url))
                                            .collect(Collectors.toList());

                                    return Flux.fromIterable(toDelete)
                                            .flatMap(storageService::deleteImage)
                                            .then();
                                })
                                .then(reportRepo.save(existing))
                                .flatMap(saved -> Flux.fromIterable(dto.getWorkshops())
                                        .flatMap(dtoW -> Flux.fromIterable(Arrays.asList(dtoW.getImageUrl()))
                                                .flatMap(image -> isBase64(image)
                                                        ? storageService.uploadBase64Image("reports/workshops", image)
                                                        : Mono.just(image))
                                                .collectList()
                                                .flatMap(images -> {
                                                    ReportWorkshop rw = fromWorkshopDto(dtoW);
                                                    rw.setReportId(saved.getId());
                                                    rw.setId(null);
                                                    rw.setImageUrl(images.toArray(new String[0]));

                                                    if (rw.getWorkshopId() != null) {
                                                        return workshopCacheRepo.findById(rw.getWorkshopId())
                                                                .map(cache -> {
                                                                    rw.setWorkshopName(cache.getName());
                                                                    rw.setWorkshopDateStart(cache.getDateStart());
                                                                    rw.setWorkshopDateEnd(cache.getDateEnd());
                                                                    return rw;
                                                                });
                                                    } else {
                                                        return Mono.just(rw);
                                                    }
                                                }))
                                        .collectList()
                                        .flatMap(newList -> workshopRepo.deleteByReportId(id)
                                                .then(workshopRepo.saveAll(newList).collectList()))
                                        .map(savedWorkshops -> {
                                            ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                            response.setReport(toDto(saved));
                                            response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                            log.info("✏️ Reporte actualizado con limpieza de imágenes: {}", response);
                                            return response;
                                        }));
                    });
                });
    }

    /**
     * 🔹 Restaurar reporte (status = A)
     */
    public Mono<Void> restore(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("A");
                    return reportRepo.save(r).then();
                });
    }

    /**
     * 🔹 Eliminación lógica (status = I)
     */
    public Mono<Void> deleteLogic(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("I");
                    return reportRepo.save(r).then();
                });
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

                    return reportRepo.findById(reportId)
                            .flatMap(report -> workshopRepo.findByReportId(reportId)
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
                                                dto.setReport_description(report.getDescription());
                                                dto.setSchedule(report.getSchedule());
                                                dto.setStatus(report.getStatus());
                                                dto.setWorkshop_id(workshop.getId());
                                                dto.setWorkshop_name(workshop.getWorkshopName());
                                                dto.setWorkshop_description(workshop.getDescription());
                                                dto.setImage_url(workshop.getImageUrl());
                                                reportData.add(dto);
                                            }

                                            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
                                            Map<String, Object> parameters = new HashMap<>();
                                            parameters.put("ReportTitle", "Reporte de Actividades");
                                            parameters.put("SUBREPORT_DIR", "images/");

                                            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
                                            byte[] pdfBytes = baos.toByteArray();

                                            // 🔄 Subir a Supabase en segundo plano (no bloquear)
                                            storageService.uploadPdf(folder, fileName, pdfBytes).subscribe();

                                            // ✅ Devolver el PDF inmediatamente
                                            HttpHeaders headers = new HttpHeaders();
                                            headers.setContentType(MediaType.APPLICATION_PDF);
                                            headers.setContentDispositionFormData("attachment", fileName);
                                            return Mono.just(new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK));

                                        } catch (Exception e) {
                                            log.error("❌ Error al generar PDF:", e);
                                            return Mono.error(new RuntimeException("Error generando el PDF", e));
                                        }
                                    })
                            ).switchIfEmpty(Mono.error(new NoSuchElementException("Reporte no encontrado con ID: " + reportId)));
                });
    }

    /**
     * ⚫ Verifica si ya existe un reporte con el mismo año y trimestre
     */
    public Mono<Boolean> existsByYearAndTrimester(Integer year, String trimester) {
        return reportRepo.findByYearAndTrimester(year, trimester)
                .hasElements();
    }

    // ======================= MAPEO DTO =======================

    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setYear(r.getYear());
        dto.setTrimester(r.getTrimester());
        dto.setDescription(r.getDescription());
        dto.setSchedule(r.getSchedule());
        dto.setStatus(r.getStatus());
        return dto;
    }

    private Report fromDto(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .description(dto.getDescription())
                .schedule(dto.getSchedule())
                .status("A")
                .build();
    }

    private ReportWorkshopDto toWorkshopDto(ReportWorkshop rw) {
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

    private ReportWorkshop fromWorkshopDto(ReportWorkshopDto dto) {
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

    private boolean isBase64(String input) {
        return input != null && input.startsWith("data:image/");
    }
}
