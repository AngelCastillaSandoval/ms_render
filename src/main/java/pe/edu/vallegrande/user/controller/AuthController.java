package pe.edu.vallegrande.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.user.service.UserService;
import reactor.core.publisher.Mono;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 🔐 Enviar enlace de restablecimiento de contraseña al correo proporcionado.
     * Este endpoint se utiliza cuando un usuario olvidó su contraseña.
     */
    @PostMapping("/forgot-password")
    public Mono<ResponseEntity<Map<String, String>>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        return userService.sendPasswordResetEmail(email)
                .map(msg -> ResponseEntity.ok(Map.of("message", msg)))
                .onErrorResume(e -> {
                    // Devuelve 404 si el correo no está registrado en Firebase o en la BD
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", e.getMessage())));
                });
    }
}
