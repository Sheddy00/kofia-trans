package hei.school.kofiatrans.service;

import hei.school.kofiatrans.db.UserRepository;
import hei.school.kofiatrans.exception.AuthenticationFailedException;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository repository = mock(UserRepository.class);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(repository, encoder);
        when(repository.findByUsername("admin"))
                .thenReturn(Optional.of(new AppUser("admin", encoder.encode("s3cret-pass"), Role.ADMIN)));
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
    }

    @Test
    void authenticate_shouldReturnUser_whenPasswordMatches() {
        AppUser user = authService.authenticate("admin", "s3cret-pass");

        assertThat(user.username()).isEqualTo("admin");
        assertThat(user.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void authenticate_shouldFail_whenPasswordIsWrong() {
        assertThatThrownBy(() -> authService.authenticate("admin", "wrong"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Identifiant ou mot de passe incorrect");
    }

    @Test
    void authenticate_shouldFailWithSameMessage_whenUserIsUnknown() {
        assertThatThrownBy(() -> authService.authenticate("ghost", "anything"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Identifiant ou mot de passe incorrect");
    }
}
