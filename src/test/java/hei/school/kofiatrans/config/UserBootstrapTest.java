package hei.school.kofiatrans.config;

import hei.school.kofiatrans.db.UserRepository;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UserBootstrapTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository repository = mock(UserRepository.class);

    @Test
    void run_shouldCreateAdminAndReadOnlyDemoWithHashedPasswords() {
        new UserBootstrap(repository, encoder,
                new UserBootstrap.Account("admin", "admin-pass"),
                new UserBootstrap.Account("demo", "demo-pass")).run(null);

        ArgumentCaptor<AppUser> users = ArgumentCaptor.forClass(AppUser.class);
        verify(repository, times(2)).createIfAbsent(users.capture());
        List<AppUser> created = users.getAllValues();
        assertThat(created).extracting(AppUser::username, AppUser::role)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("admin", Role.ADMIN),
                        org.assertj.core.groups.Tuple.tuple("demo", Role.VIEWER));
        assertThat(created.getFirst().passwordHash()).isNotEqualTo("admin-pass");
        assertThat(encoder.matches("admin-pass", created.getFirst().passwordHash())).isTrue();
    }

    @Test
    void run_shouldSkipAccountsThatAreNotConfigured() {
        new UserBootstrap(repository, encoder,
                new UserBootstrap.Account("", ""),
                new UserBootstrap.Account("demo", " ")).run(null);

        verify(repository, never()).createIfAbsent(any());
    }
}
