package hei.school.kofiatrans.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCorsFilterTest {

    private static final String FRONTEND = "https://kofia-trans-ui.onrender.com";

    private final ApiCorsFilter filter = new ApiCorsFilter("http://localhost:5173, " + FRONTEND);

    @Test
    void preflight_fromAllowedOrigin_shouldBeAnsweredWithoutReachingChain() throws Exception {
        MockHttpServletRequest request = preflight(FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(FRONTEND);
        assertThat(response.getHeader("Access-Control-Allow-Headers")).containsIgnoringCase("authorization");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void preflight_fromUnknownOrigin_shouldBeRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(preflight("https://evil.example"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void actualRequest_fromAllowedOrigin_shouldContinueChainWithCorsHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/trips");
        request.addHeader("Origin", FRONTEND);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(FRONTEND);
    }

    private static MockHttpServletRequest preflight(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/trips");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "authorization");
        return request;
    }
}
