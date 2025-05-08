package pe.edu.vallegrande.actividad.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.actividad.dto.ActivityLogDto;
import pe.edu.vallegrande.actividad.model.ActivityLog;
import pe.edu.vallegrande.actividad.service.ActivityLogService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService service;

    @GetMapping("/log")
    public Flux<ActivityLog> getAll() {
        return service.findAll();
    }

    @PostMapping("/log")
    public Mono<ActivityLog> create(@RequestBody ActivityLogDto dto) {
        return service.create(dto);
    }

    @DeleteMapping("/log/{id}")
    public Mono<Void> delete(@PathVariable UUID id) {
        return service.deleteById(id);
    }
}
