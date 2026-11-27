package hei.school.kofiatrans.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldContinueChain_whenApiKeyIsValid() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret", objectMapper);
        when(request.getHeader("x-api-key")).thenReturn("secret");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void shouldReturnForbidden_whenApiKeyIsMissing() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret", objectMapper);
        when(request.getHeader("x-api-key")).thenReturn(null);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(body.toString()).contains("Forbidden");
    }

    @Test
    void shouldReturnForbidden_whenApiKeyIsWrong() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret", objectMapper);
        when(request.getHeader("x-api-key")).thenReturn("wrong-key");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldReturnForbidden_whenExpectedApiKeyIsNotConfigured() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("", objectMapper);
        when(request.getHeader("x-api-key")).thenReturn("anything");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }
}
