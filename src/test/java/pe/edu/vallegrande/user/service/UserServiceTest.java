package pe.edu.vallegrande.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.vallegrande.user.dto.UserDto;
import pe.edu.vallegrande.user.model.User;
import pe.edu.vallegrande.user.repository.UsersRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UsersRepository usersRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        usersRepository = mock(UsersRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        userService = new UserService(usersRepository, passwordEncoder, emailService);
    }

    @Test
    void shouldReturnUserProfile_whenUidExists() {
        // Arrange
        String uid = "abc123";
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setFirebaseUid(uid);
        mockUser.setEmail("test@email.com");
        mockUser.setName("Angel");
        mockUser.setRole(List.of("USER")); // ✅ Lista de roles

        when(usersRepository.findAll()).thenReturn(Mono.just(mockUser).flux());

        // Act
        Mono<UserDto> result = userService.findMyProfile(uid);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(userDto ->
                        userDto.getFirebaseUid().equals(uid) &&
                                userDto.getEmail().equals("test@email.com") &&
                                userDto.getName().equals("Angel") &&
                                userDto.getRole().contains("USER") // ✅ Verifica rol en lista
                )
                .verifyComplete();
    }
}
