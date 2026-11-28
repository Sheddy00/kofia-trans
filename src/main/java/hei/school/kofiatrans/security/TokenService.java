package hei.school.kofiatrans.security;

import hei.school.kofiatrans.model.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Issues signed access tokens (HS256) carrying the username as subject and the role as a claim. */
public class TokenService {
    public static final String ISSUER = "kofia-trans";
    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, Duration expiration, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
        this.clock = clock;
    }

    public IssuedToken issue(AppUser user) {
        // JWT timestamps have second precision; truncate so the response matches the token exactly.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(expiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt, expiration.toSeconds());
    }

    public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
