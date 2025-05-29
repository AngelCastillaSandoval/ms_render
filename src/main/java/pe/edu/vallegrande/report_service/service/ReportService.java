package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository repository;

    // 🔍 Listar todos
    public Flux<ReportDto> findAll() {
        return repository.findAll().map(this::toDto);
    }

    // 🔍 Buscar por ID
    public Mono<ReportDto> findById(Integer id) {
        return repository.findById(id).map(this::toDto);
    }

    // ✅ Insertar con validación de duplicado
    public Mono<ReportDto> save(ReportDto dto) {
        return repository.findAllByYearAndTrimester(dto.getYear(), dto.getTrimester())
                .filter(report -> "A".equals(report.getStatus()))
                .hasElements()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalStateException("Ya existe un reporte activo para ese año y trimestre."));
                    }
                    Report entity = toEntity(dto);
                    return repository.save(entity).map(this::toDto);
                });
    }

    // 🛠️ Actualizar sin manejar archivos
    public Mono<ReportDto> update(Integer id, ReportDto dto) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setYear(dto.getYear());
                    existing.setTrimester(dto.getTrimester());
                    existing.setDescriptionUrl(dto.getDescriptionUrl());
                    existing.setScheduleUrl(dto.getScheduleUrl());
                    existing.setStatus(dto.getStatus());

                    return repository.save(existing).map(this::toDto);
                });
    }

    // ❌ Borrado lógico
    public Mono<Void> disable(Integer id) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setStatus("I");
                    existing.setScheduleUrl(null); // opcional: limpiar el archivo
                    return repository.save(existing).then();
                });
    }

    // ✅ Restaurar reporte
    public Mono<Void> restore(Integer id) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setStatus("A");
                    return repository.save(existing).then();
                });
    }

    // 🗑️ Eliminado físico
    public Mono<Void> delete(Integer id) {
        return repository.findById(id)
                .flatMap(repository::delete);
    }

    // ❓ Validar duplicado sin insertar
    public Mono<Boolean> existsByYearAndTrimester(Integer year, String trimester) {
        return repository.findAllByYearAndTrimester(year, trimester)
                .filter(report -> "A".equals(report.getStatus()))
                .hasElements();
    }

    // 🔁 Mapper: entidad → DTO
    private ReportDto toDto(Report report) {
        ReportDto dto = new ReportDto();
        dto.setId(report.getId());
        dto.setYear(report.getYear());
        dto.setTrimester(report.getTrimester());
        dto.setDescriptionUrl(report.getDescriptionUrl());
        dto.setScheduleUrl(report.getScheduleUrl());
        dto.setStatus(report.getStatus());
        return dto;
    }

    // 🔁 Mapper: DTO → entidad
    private Report toEntity(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .descriptionUrl(dto.getDescriptionUrl())
                .scheduleUrl(dto.getScheduleUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "A")
                .build();
    }
}
