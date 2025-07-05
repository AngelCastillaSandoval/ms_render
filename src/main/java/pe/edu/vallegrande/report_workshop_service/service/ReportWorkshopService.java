package pe.edu.vallegrande.report_workshop_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.dto.ReportAttendanceSummaryDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_workshop_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_workshop_service.model.*;
import pe.edu.vallegrande.report_workshop_service.repository.ReportAttendanceSummaryRepository;
import pe.edu.vallegrande.report_workshop_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.AttendanceCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.IssueCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.PersonCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.WorkshopCacheRepository;
import pe.edu.vallegrande.report_workshop_service.webclient.ReportCoreClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportWorkshopService {

    private final ReportCoreClient reportClient;
    private final ReportWorkshopRepository reportWorkshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;
    private final PersonCacheRepository personCacheRepo;
    private final AttendanceCacheRepository attendanceRepo;
    private final ReportAttendanceSummaryRepository summaryRepo;
    private final IssueCacheRepository issueCacheRepo;

    // Devuelve el orden numérico del trimestre para ordenar reportes
    private int getTrimesterOrder(String trimester) {
        return switch (trimester.toLowerCase()) {
            case "enero-marzo" -> 1;
            case "abril-junio" -> 2;
            case "julio-septiembre" -> 3;
            case "octubre-diciembre" -> 4;
            default -> 5;
        };
    }

    // Lista reportes filtrados por estado, trimestre, año y fechas de talleres
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

    // Busca un reporte por ID incluyendo sus talleres filtrados por fechas
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

    // Construye el DTO de taller y agrega resumen de asistencia si corresponde
    private Mono<ReportWorkshopDto> buildDtoWithDateFilter(ReportWorkshop rw, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        ReportWorkshopDto dto = toDto(rw);

        if (rw.getWorkshopId() != null) {
            return workshopCacheRepo.findById(rw.getWorkshopId())
                    .flatMap(wc -> {
                        boolean inRange = true;
                        if (workshopDateStart != null) inRange = !wc.getStartDate().isBefore(workshopDateStart);
                        if (workshopDateEnd != null) inRange = inRange && !wc.getEndDate().isAfter(workshopDateEnd);
                        if (!inRange) return Mono.empty();

                        dto.setWorkshopStatus(wc.getState());
                        dto.setWorkshopDateStart(wc.getStartDate());
                        dto.setWorkshopDateEnd(wc.getEndDate());
                        dto.setWorkshopName(wc.getName());

                        return summaryRepo.findByReportWorkshopId(rw.getId())
                                .collectList()
                                .map(summaries -> {
                                    dto.setAttendanceSummaries(summaries.stream().map(this::toDto).toList());
                                    return dto;
                                });
                    })
                    // Si no está en el cache, igual carga el resumen de la base
                    .switchIfEmpty(summaryRepo.findByReportWorkshopId(rw.getId())
                            .collectList()
                            .map(summaries -> {
                                dto.setAttendanceSummaries(summaries.stream().map(this::toDto).toList());
                                return dto;
                            }));
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

    // Crea un nuevo reporte con talleres y resumen de asistencia (si tiene workshopId)
    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        return reportClient.create(dto.getReport())
                .flatMap(savedReport -> saveWorkshops(savedReport.getId(), dto.getWorkshops()).collectList()
                        .map(savedWorkshops -> {
                            ReportWithWorkshopsDto result = new ReportWithWorkshopsDto();
                            result.setReport(savedReport);
                            result.setWorkshops(savedWorkshops.stream().map(this::toDto).toList());
                            return result;
                        })
                );
    }

    // Actualiza un reporte y reemplaza sus talleres y resúmenes
    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportClient.update(id, dto.getReport())
                .flatMap(updatedReport -> reportWorkshopRepo.deleteByReportId(id)
                        .then(saveWorkshops(id, dto.getWorkshops()).collectList())
                        .map(savedWorkshops -> {
                            ReportWithWorkshopsDto result = new ReportWithWorkshopsDto();
                            result.setReport(updatedReport);
                            result.setWorkshops(savedWorkshops.stream().map(this::toDto).toList());
                            return result;
                        })
                );
    }

    // Desactiva un reporte
    public Mono<Void> disable(Integer id) {
        return reportClient.disable(id);
    }

    // Restaura un reporte desactivado
    public Mono<Void> restore(Integer id) {
        return reportClient.restore(id);
    }

    // Elimina un reporte con sus talleres y resúmenes asociados
    public Mono<Void> delete(Integer id) {
        return reportWorkshopRepo.findByReportId(id)
                .map(ReportWorkshop::getId)
                .collectList()
                .flatMapMany(summaryRepo::deleteByReportWorkshopIdIn)
                .then(reportWorkshopRepo.deleteByReportId(id))
                .then(reportClient.delete(id));
    }

    // Guarda los talleres del reporte, con resumen si corresponde
    private Flux<ReportWorkshop> saveWorkshops(Integer reportId, List<ReportWorkshopDto> dtos) {
        return Flux.fromIterable(dtos)
                .flatMap(dto -> {
                    ReportWorkshop rw = fromDto(dto);
                    rw.setReportId(reportId);

                    if (rw.getWorkshopId() != null) {
                        return buildWithSummary(rw);
                    }
                    return reportWorkshopRepo.save(rw);
                });
    }

    // Construye taller con resumen de asistencia consultando datos del cache
    private Mono<ReportWorkshop> buildWithSummary(ReportWorkshop rw) {
        return workshopCacheRepo.findById(rw.getWorkshopId())
                .flatMap(cache -> {
                    rw.setWorkshopName(cache.getName());
                    rw.setWorkshopDateStart(cache.getStartDate());
                    rw.setWorkshopDateEnd(cache.getEndDate());

                    List<Integer> personIds = Stream.of(cache.getPersonId().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .toList();

                    return issueCacheRepo.findAllByWorkshopId(rw.getWorkshopId())
                            .map(IssueCache::getId)
                            .collectList()
                            .flatMap(issueIds -> Flux.fromIterable(personIds)
                                    .flatMap(personId -> Mono.zip(
                                            personCacheRepo.findById(personId),
                                            attendanceRepo.findAllByIssueIdInAndPersonId(issueIds, personId).collectList()
                                    ).map(tuple -> buildSummary(personId, tuple.getT1(), tuple.getT2())))
                                    .collectList())
                            .flatMap(summaries -> reportWorkshopRepo.save(rw)
                                    .flatMap(saved -> {
                                        summaries.forEach(s -> s.setReportWorkshopId(saved.getId()));
                                        return summaryRepo.saveAll(summaries).collectList().thenReturn(saved);
                                    }));
                });
    }

    // Genera un resumen de asistencia por persona
    private ReportAttendanceSummary buildSummary(Integer personId, PersonCache person, List<AttendanceCache> records) {
        return ReportAttendanceSummary.builder()
                .reportWorkshopId(null)
                .personId(personId)
                .personName(person.getName() + " " + person.getSurname())
                .presentCount((int) records.stream().filter(a -> "A".equalsIgnoreCase(a.getRecord())).count())
                .absentCount((int) records.stream().filter(a -> "F".equalsIgnoreCase(a.getRecord())).count())
                .lateCount((int) records.stream().filter(a -> "T".equalsIgnoreCase(a.getRecord())).count())
                .justifiedCount((int) records.stream().filter(a -> "J".equalsIgnoreCase(a.getRecord())).count())
                .build();
    }

    // Convierte entidad a DTO para respuesta al cliente
    private ReportWorkshopDto toDto(ReportWorkshop rw) {
        return ReportWorkshopDto.builder()
                .id(rw.getId())
                .reportId(rw.getReportId())
                .workshopId(rw.getWorkshopId())
                .workshopName(rw.getWorkshopName())
                .workshopDateStart(rw.getWorkshopDateStart())
                .workshopDateEnd(rw.getWorkshopDateEnd())
                .description(rw.getDescription())
                .imageUrl(rw.getImageUrl())
                .build();
    }

    // Convierte DTO a entidad para persistencia
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

    // Convierte entidad de resumen a DTO para mostrar en frontend
    private ReportAttendanceSummaryDto toDto(ReportAttendanceSummary summary) {
        return ReportAttendanceSummaryDto.builder()
                .id(summary.getId())
                .reportWorkshopId(summary.getReportWorkshopId())
                .personId(summary.getPersonId())
                .personName(summary.getPersonName())
                .presentCount(summary.getPresentCount())
                .absentCount(summary.getAbsentCount())
                .lateCount(summary.getLateCount())
                .justifiedCount(summary.getJustifiedCount())
                .build();
    }
}