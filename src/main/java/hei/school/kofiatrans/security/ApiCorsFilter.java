package hei.school.kofiatrans.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Runs before the Spring Security filter chain so that browser preflight requests are answered without a token
 * and error responses (403, 404...) still carry CORS headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiCorsFilter extends CorsFilter {

    public ApiCorsFilter(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        super(corsConfigurationSource(allowedOrigins));
    }

    private static UrlBasedCorsConfigurationSource corsConfigurationSource(String allowedOrigins) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
