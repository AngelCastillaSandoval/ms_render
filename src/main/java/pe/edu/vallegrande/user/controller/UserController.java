package pe.edu.vallegrande.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.user.config.CustomAuthenticationToken;
import pe.edu.vallegrande.user.dto.UserDto;
import pe.edu.vallegrande.user.service.UserService;
import reactor.core.publisher.Mono;

import java.util.Map;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 🔍 Obtener mi perfil por UID
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Mono<UserDto> getMyProfile(@AuthenticationPrincipal CustomAuthenticationToken auth) {
        return userService.findMyProfile(auth.getName());
    }

    /**
     * ✏️ Editar mis propios datos (excepto email/password/rol)
     */
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Mono<Map<String, Object>> updateMyProfile(@AuthenticationPrincipal CustomAuthenticationToken auth,
                                                     @RequestBody Map<String, Object> payload) {
        String uid = auth.getName();
        if (payload.get("name") == null || payload.get("lastName") == null || payload.get("documentNumber") == null) {
            return Mono.just(Map.of("error", "Faltan campos obligatorios"));
        }

        UserDto dto = new UserDto();
        dto.setName((String) payload.get("name"));
        dto.setLastName((String) payload.get("lastName"));
        dto.setDocumentType((String) payload.get("documentType"));
        dto.setDocumentNumber((String) payload.get("documentNumber"));
        dto.setCellPhone((String) payload.get("cellPhone"));
        dto.setProfileImage((String) payload.get("profileImage"));

        return userService.updateMyProfile(uid, dto)
                .map(user -> Map.of("message", "✅ Perfil actualizado correctamente", "user", user))
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    /**
     * 🔁 Cambiar mi contraseña
     */
    @PutMapping("/password")
    public Mono<UserDto> changePassword(@AuthenticationPrincipal CustomAuthenticationToken auth,
                                        @RequestBody Map<String, String> body) {
        return userService.changePassword(auth.getName(), body.get("newPassword"));
    }

    /**
     * 🔁 Cambiar mi correo electrónico
     */
    @PutMapping("/email")
    public Mono<UserDto> changeEmail(@AuthenticationPrincipal CustomAuthenticationToken auth,
                                     @RequestBody Map<String, String> body) {
        return userService.changeEmail(auth.getName(), body.get("newEmail"));
    }
}
