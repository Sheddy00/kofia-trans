package hei.school.kofiatrans.config;

import hei.school.kofiatrans.db.UserRepository;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the accounts configured through environment variables on startup, so no password hash
 * ever has to be written by hand. Existing accounts are left untouched.
 */
public class UserBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(UserBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Account admin;
    private final Account demo;

    public UserBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder, Account admin, Account demo) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.admin = admin;
        this.demo = demo;
    }

    @Override
    public void run(ApplicationArguments args) {
        create(admin, Role.ADMIN);
        create(demo, Role.VIEWER);
    }

    private void create(Account account, Role role) {
        if (!account.isConfigured()) {
            return;
        }
        boolean created = userRepository.createIfAbsent(
                new AppUser(account.username(), passwordEncoder.encode(account.password()), role));
        if (created) {
            log.info("Created {} account '{}'", role, account.username());
        }
    }

    public record Account(String username, String password) {
        public boolean isConfigured() {
            return username != null && !username.isBlank() && password != null && !password.isBlank();
        }
    }
}
