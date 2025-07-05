package pe.edu.vallegrande.report_workshop_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_workshop_service.dto.PreviewAttendanceSummaryDto;
import pe.edu.vallegrande.report_workshop_service.model.AttendanceCache;
import pe.edu.vallegrande.report_workshop_service.model.IssueCache;
import pe.edu.vallegrande.report_workshop_service.repository.cache.AttendanceCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.IssueCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.PersonCacheRepository;
import pe.edu.vallegrande.report_workshop_service.repository.cache.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceCacheService {

    private final AttendanceCacheRepository attendanceCacheRepository;
    private final IssueCacheRepository issueCacheRepository;
    private final PersonCacheRepository personCacheRepository;
    private final WorkshopCacheRepository workshopCacheRepository;


    /**
     * Obtiene un resumen de asistencia previo por cada persona que participó en un taller.
     *
     * workshopId ID del taller
     * Lista reactiva con los resúmenes por persona
     */
    public Flux<PreviewAttendanceSummaryDto> getPreviewSummaryByWorkshopId(Integer workshopId) {
        return workshopCacheRepository.findById(workshopId)
                .flatMapMany(workshop -> {
                    // 1. Extraer IDs de personas en el taller
                    List<Integer> personIds = Arrays.stream(workshop.getPersonId().split(","))
                            .map(String::trim)
                            .map(Integer::valueOf)
                            .toList();

                    // 2. Buscar todos los temas del taller
                    return issueCacheRepository.findAllByWorkshopId(workshopId)
                            .map(IssueCache::getId)
                            .collectList()
                            .flatMapMany(issueIds -> {
                                // Si no hay temas, aún así se deben mostrar personas con 0s
                                return Flux.fromIterable(personIds)
                                        .flatMap(personId -> {
                                            Mono<List<AttendanceCache>> attendanceMono;

                                            if (issueIds.isEmpty()) {
                                                attendanceMono = Mono.just(Collections.emptyList());
                                            } else {
                                                attendanceMono = attendanceCacheRepository
                                                        .findAllByIssueIdInAndPersonId(issueIds, personId)
                                                        .collectList();
                                            }

                                            return Mono.zip(
                                                    personCacheRepository.findById(personId),
                                                    attendanceMono
                                            ).map(tuple -> {
                                                var person = tuple.getT1();
                                                var attendances = tuple.getT2();

                                                long present = attendances.stream().filter(r -> "A".equals(r.getRecord())).count();
                                                long absent = attendances.stream().filter(r -> "F".equals(r.getRecord())).count();
                                                long late = attendances.stream().filter(r -> "T".equals(r.getRecord())).count();
                                                long justified = attendances.stream().filter(r -> "J".equals(r.getRecord())).count();

                                                return PreviewAttendanceSummaryDto.builder()
                                                        .personId(personId)
                                                        .personName(person.getName() + " " + person.getSurname())
                                                        .presentCount((int) present)
                                                        .absentCount((int) absent)
                                                        .lateCount((int) late)
                                                        .justifiedCount((int) justified)
                                                        .build();
                                            });
                                        });
                            });
                });
    }
}
