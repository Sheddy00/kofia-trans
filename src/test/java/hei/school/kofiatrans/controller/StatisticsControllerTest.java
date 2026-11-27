package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.service.RevenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private RevenueService revenueService;

    @InjectMocks
    private StatisticsController statisticsController;

    @Test
    void getTopEarningDriver_shouldThrow_whenParamsMissing() {
        assertThrows(ClientException.class, () -> statisticsController.getTopEarningDriver(null, "2026-01-31"));
    }

    @Test
    void getTopEarningDriver_shouldThrow_whenDateInvalid() {
        assertThrows(ClientException.class, () -> statisticsController.getTopEarningDriver("not-a-date", "2026-01-31"));
    }

    @Test
    void getTopEarningDriver_shouldReturnNotFound_whenNoDriver() {
        when(revenueService.findTopEarningDriver(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(null);

        ResponseEntity<?> response = statisticsController.getTopEarningDriver("2026-01-01", "2026-01-31");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTopEarningDriver_shouldReturnDriverAndRevenue() {
        Driver driver = new Driver("d1", "Alice", null, null);
        when(revenueService.findTopEarningDriver(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(driver);
        when(revenueService.computeDriverRevenue("d1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(250000L);

        ResponseEntity<?> response = statisticsController.getTopEarningDriver("2026-01-01", "2026-01-31");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
