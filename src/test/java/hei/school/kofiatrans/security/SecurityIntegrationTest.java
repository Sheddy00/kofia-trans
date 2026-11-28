package hei.school.kofiatrans.security;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.AuthenticationFailedException;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;
import hei.school.kofiatrans.service.AuthService;
import hei.school.kofiatrans.service.RevenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real security filter chain in front of the real controllers. */
@WebMvcTest(properties = {
        "app.jwt.secret=integration-test-secret-long-enough-for-hs256",
        "app.cors.allowed-origins=https://kofia-trans-ui.onrender.com"
})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    private static final String STATUS_BODY = "{\"status\":\"CANCELLED\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private DataRetriever dataRetriever;

    @MockitoBean
    private RevenueService revenueService;

    @MockitoBean
    private AuthService authService;

    private String bearer(Role role) {
        return "Bearer " + tokenService.issue(new AppUser(role.name().toLowerCase(), "hash", role)).value();
    }

    @Test
    void readWithoutToken_shouldReturn401Json() throws Exception {
        mockMvc.perform(get("/trips"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void readWithForgedToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/trips").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readAsViewer_shouldBeAllowed() throws Exception {
        when(dataRetriever.findAllTrips()).thenReturn(List.of());

        mockMvc.perform(get("/trips").header("Authorization", bearer(Role.VIEWER)))
                .andExpect(status().isOk());
    }

    @Test
    void writeAsViewer_shouldReturn403Json() throws Exception {
        mockMvc.perform(put("/trips/t-001/status")
                        .header("Authorization", bearer(Role.VIEWER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Action réservée aux administrateurs"));
    }

    @Test
    void writeAsAdmin_shouldReachController() throws Exception {
        when(dataRetriever.findTripById("t-404")).thenReturn(null);

        mockMvc.perform(put("/trips/t-404/status")
                        .header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course introuvable : t-404"));
    }

    @Test
    void login_shouldBePublicAndReturnBearerToken() throws Exception {
        when(authService.authenticate("demo", "demo-pass")).thenReturn(new AppUser("demo", "hash", Role.VIEWER));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("demo"))
                .andExpect(jsonPath("$.user.role").value("VIEWER"));
    }

    @Test
    void loginWithBadCredentials_shouldReturn401Json() throws Exception {
        when(authService.authenticate("demo", "wrong"))
                .thenThrow(new AuthenticationFailedException("Identifiant ou mot de passe incorrect"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect"));
    }

    @Test
    void loginWithMissingFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void unknownRoute_shouldReturn401WithoutTokenAnd404WithToken() throws Exception {
        mockMvc.perform(get("/unknown")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/unknown").header("Authorization", bearer(Role.VIEWER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsPreflight_shouldBeAnsweredWithoutToken() throws Exception {
        mockMvc.perform(options("/trips")
                        .header("Origin", "https://kofia-trans-ui.onrender.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://kofia-trans-ui.onrender.com"));
    }

    @Test
    void deleteAsViewer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/drivers/d-001").header("Authorization", bearer(Role.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAsAdmin_shouldReachController() throws Exception {
        when(dataRetriever.deleteTrip("t-001")).thenReturn(true);

        mockMvc.perform(delete("/trips/t-001").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void conflictOnDelete_shouldReturn409Json() throws Exception {
        when(dataRetriever.deleteVehicle("v-001"))
                .thenThrow(new hei.school.kofiatrans.exception.ConflictException("Impossible de supprimer ce véhicule"));

        mockMvc.perform(delete("/vehicles/v-001").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void pagedTrips_shouldExposeTotalCountToTheBrowser() throws Exception {
        when(dataRetriever.countTrips(null, null, null)).thenReturn(21L);
        when(dataRetriever.findTripsPage(null, null, null, 5, 0)).thenReturn(List.of());

        mockMvc.perform(get("/trips").param("size", "5")
                        .header("Authorization", bearer(Role.VIEWER))
                        .header("Origin", "https://kofia-trans-ui.onrender.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "21"))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Total-Count"));
    }
}
