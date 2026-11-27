package hei.school.kofiatrans.service;

import hei.school.kofiatrans.db.UserRepository;
import hei.school.kofiatrans.exception.AuthenticationFailedException;
import hei.school.kofiatrans.model.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthService {
    private static final String INVALID_CREDENTIALS = "Identifiant ou mot de passe incorrect";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // Compared against when the user does not exist, so both failure paths take the same time.
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("kofia-trans-timing-guard");
    }

    public AppUser authenticate(String username, String password) {
        AppUser user = userRepository.findByUsername(username).orElse(null);
        String hash = user != null ? user.passwordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(password, hash);
        if (user == null || !matches) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return user;
    }
}
