package pe.edu.vallegrande.actividad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.actividad.dto.ActivityLogDto;
import pe.edu.vallegrande.actividad.model.ActivityLog;
import pe.edu.vallegrande.actividad.repository.ActivityLogRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository repository;

    public Flux<ActivityLog> findAll() {
        return repository.findAllByOrderByFechaDesc();
    }

    public Mono<ActivityLog> create(ActivityLogDto dto) {
        ActivityLog entity = new ActivityLog();
        entity.setImagen(dto.getImagen());
        entity.setNombre(dto.getNombre());
        entity.setModulo(dto.getModulo());
        entity.setAccion(dto.getAccion());
        entity.setFecha(LocalDateTime.now());
        return repository.save(entity);
    }

    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }
}
