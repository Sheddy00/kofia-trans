package hei.school.kofiatrans.security;

import tools.jackson.databind.ObjectMapper;
import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "x-api-key";

    private final String expectedApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${app.api-key:}") String expectedApiKey, ObjectMapper objectMapper) {
        this.expectedApiKey = expectedApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        if (expectedApiKey.isEmpty() || !expectedApiKey.equals(providedApiKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ErrorResponseDto.of(403, "Forbidden", "Clé API manquante ou invalide")
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
