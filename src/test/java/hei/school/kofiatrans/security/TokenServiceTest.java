package hei.school.kofiatrans.security;

import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256!";
    private final SecurityConfig config = new SecurityConfig();
    private final SecretKey key = config.jwtSigningKey(SECRET);
    private final JwtDecoder decoder = config.jwtDecoder(key);
    private final AppUser admin = new AppUser("admin", "hash", Role.ADMIN);

    private TokenService serviceAt(Instant now) {
        return new TokenService(config.jwtEncoder(key), Duration.ofHours(8), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void issuedToken_shouldCarryUsernameRoleAndExpiry() {
        TokenService.IssuedToken token = serviceAt(Instant.now()).issue(admin);

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenService.ISSUER);
        assertThat(token.expiresInSeconds()).isEqualTo(8 * 3600);
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void expiredToken_shouldBeRejected() {
        TokenService.IssuedToken token = serviceAt(Instant.now().minus(Duration.ofHours(9))).issue(admin);

        assertThatThrownBy(() -> decoder.decode(token.value())).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_shouldBeRejected() {
        SecretKey otherKey = config.jwtSigningKey("another-secret-that-is-long-enough-too!!");
        TokenService other = new TokenService(config.jwtEncoder(otherKey), Duration.ofHours(1), Clock.systemUTC());

        assertThatThrownBy(() -> decoder.decode(other.issue(admin).value())).isInstanceOf(JwtException.class);
    }

    @Test
    void tooShortSecret_shouldFailFast() {
        assertThatThrownBy(() -> config.jwtSigningKey("short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
