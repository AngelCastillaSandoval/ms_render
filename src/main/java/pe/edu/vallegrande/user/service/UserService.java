package pe.edu.vallegrande.user.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.user.dto.UserCreateDto;
import pe.edu.vallegrande.user.dto.UserDto;
import pe.edu.vallegrande.user.model.User;
import pe.edu.vallegrande.user.repository.UsersRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public UserService(UsersRepository usersRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Crear un nuevo usuario: registra en Firebase y luego en la base de datos.
     */
    public Mono<UserDto> createUser(UserCreateDto dto) {
        return emailExists(dto.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("❌ El correo ya está registrado."));
                    }

                    CreateRequest request = new CreateRequest()
                            .setEmail(dto.getEmail())
                            .setPassword(dto.getPassword())
                            .setEmailVerified(false)
                            .setDisabled(false);

                    return Mono.fromCallable(() -> FirebaseAuth.getInstance().createUser(request))
                            .flatMap(firebaseUser -> {
                                String uid = firebaseUser.getUid();
                                String primaryRole = dto.getRole().isEmpty() ? "USER" : dto.getRole().get(0);
                                return Mono.fromCallable(() -> {
                                    FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", primaryRole.toUpperCase()));
                                    return uid;
                                });
                            })
                            .flatMap(uid -> {
                                User user = new User();
                                user.setFirebaseUid(uid);
                                user.setName(dto.getName());
                                user.setLastName(dto.getLastName());
                                user.setDocumentType(dto.getDocumentType());
                                user.setDocumentNumber(dto.getDocumentNumber());
                                user.setCellPhone(dto.getCellPhone());
                                user.setEmail(dto.getEmail());
                                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                                user.setRole(dto.getRole());
                                user.setProfileImage(dto.getProfileImage());
                                return usersRepository.save(user).map(this::toDto);
                            });
                });
    }

    /**
     * Actualiza datos de un usuario por ID, sin modificar email ni contraseña.
     */
    public Mono<UserDto> updateUser(Integer id, UserDto dto) {
        return usersRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                .flatMap(existing -> {
                    existing.setName(dto.getName());
                    existing.setLastName(dto.getLastName());
                    existing.setDocumentType(dto.getDocumentType());
                    existing.setDocumentNumber(dto.getDocumentNumber());
                    existing.setCellPhone(dto.getCellPhone());
                    existing.setRole(dto.getRole());
                    if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
                        existing.setProfileImage(dto.getProfileImage());
                    }
                    return usersRepository.save(existing).map(this::toDto);
                });
    }

    /**
     * Devuelve los datos del usuario actual por su UID de Firebase.
     */
    public Mono<UserDto> findMyProfile(String firebaseUid) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .map(this::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Devuelve todos los usuarios registrados.
     */
    public Flux<UserDto> findAllUsers() {
        return usersRepository.findAll().map(this::toDto);
    }

    /**
     * Devuelve un usuario por su ID.
     */
    public Mono<UserDto> findById(Integer id) {
        return usersRepository.findById(id).map(this::toDto);
    }

    /**
     * Devuelve un usuario por su email.
     */
    public Mono<UserDto> findByEmail(String email) {
        return usersRepository.findByEmail(email).map(this::toDto);
    }

    /**
     * Verifica si un email ya está registrado.
     */
    public Mono<Boolean> emailExists(String email) {
        return findByEmail(email).hasElement();
    }

    /**
     * Elimina un usuario por ID de Firebase y la base de datos.
     */
    public Mono<Void> deleteUser(Integer id) {
        return usersRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> {
                    String firebaseUid = user.getFirebaseUid();
                    Mono<Void> firebaseDeletion = Mono.fromCallable(() -> {
                        FirebaseAuth.getInstance().deleteUser(firebaseUid);
                        return null;
                    });
                    Mono<Void> dbDeletion = usersRepository.deleteById(user.getId());
                    return firebaseDeletion.then(dbDeletion);
                });
    }

    /**
     * Cambia el email del usuario en Firebase y en la base de datos.
     */
    public Mono<UserDto> changeEmail(String firebaseUid, String newEmail) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> usersRepository.findByEmail(newEmail)
                        .flatMap(conflict -> Mono.error(new RuntimeException("El correo ya está en uso")))
                        .switchIfEmpty(Mono.defer(() -> Mono.fromCallable(() -> {
                            FirebaseAuth.getInstance().updateUser(
                                    new com.google.firebase.auth.UserRecord.UpdateRequest(firebaseUid)
                                            .setEmail(newEmail)
                            );
                            return user;
                        }))))
                .flatMap(obj -> {
                    User user = (User) obj;
                    user.setEmail(newEmail);
                    return usersRepository.save(user).map(this::toDto);
                });

    }

    /**
     * Cambia la contraseña en Firebase y la actualiza en la base de datos.
     */
    public Mono<UserDto> changePassword(String firebaseUid, String newPassword) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> Mono.fromCallable(() -> {
                    FirebaseAuth.getInstance().updateUser(
                            new com.google.firebase.auth.UserRecord.UpdateRequest(firebaseUid)
                                    .setPassword(newPassword)
                    );
                    return user;
                }))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return usersRepository.save(user).map(this::toDto);
                });
    }

    /**
     * Envía un correo con enlace para reestablecer contraseña.
     */
    public Mono<String> sendPasswordResetEmail(String email) {
        return Mono.fromCallable(() -> FirebaseAuth.getInstance().getUserByEmail(email))
                .flatMap(userRecord -> usersRepository.findByEmail(email)
                        .switchIfEmpty(Mono.error(new RuntimeException("❌ El email no está registrado en el sistema.")))
                        .flatMap(user -> Mono.fromCallable(() -> {
                            String link = FirebaseAuth.getInstance().generatePasswordResetLink(email);
                            emailService.sendResetLink(email, link);
                            return "✅ Enlace enviado correctamente a: " + email;
                        })))
                .onErrorResume(e -> {
                    log.error("❌ Error real desde Firebase: ", e);
                    String msg = e.getMessage().contains("NOT_FOUND")
                            ? "❌ El correo no existe en Firebase"
                            : "⚠️ Error: " + e.getMessage();
                    return Mono.error(new RuntimeException(msg));
                });
    }

    /**
     * Permite al usuario editar su propio perfil sin cambiar email, password ni rol.
     */
    public Mono<UserDto> updateMyProfile(String uid, UserDto updatedData) {
        return usersRepository.findByFirebaseUid(uid)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(existing -> {
                    existing.setName(updatedData.getName());
                    existing.setLastName(updatedData.getLastName());
                    existing.setDocumentType(updatedData.getDocumentType());
                    existing.setDocumentNumber(updatedData.getDocumentNumber());
                    existing.setCellPhone(updatedData.getCellPhone());
                    existing.setProfileImage(updatedData.getProfileImage());
                    return usersRepository.save(existing);
                })
                .map(UserDto::fromEntity);
    }

    /**
     * Convierte la entidad User a UserDto
     */
    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirebaseUid(),
                user.getName(),
                user.getLastName(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getCellPhone(),
                user.getEmail(),
                user.getRole(),
                user.getProfileImage()
        );
    }
}
