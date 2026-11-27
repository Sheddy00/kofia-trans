package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.db.DataRetriever;
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
class DriverControllerTest {

    @Mock
    private DataRetriever dataRetriever;

    @Mock
    private RevenueService revenueService;

    @InjectMocks
    private DriverController driverController;

    @Test
    void getRevenue_shouldThrow_whenDatesMissing() {
        assertThrows(ClientException.class, () -> driverController.getRevenue("d1", null, "2026-01-31"));
    }

    @Test
    void getRevenue_shouldReturnNotFound_whenDriverMissing() {
        when(dataRetriever.findDriverById("d1")).thenReturn(null);

        ResponseEntity<?> response = driverController.getRevenue("d1", "2026-01-01", "2026-01-31");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponseDto.class);
    }

    @Test
    void getRevenue_shouldReturnRevenueAndFee() {
        Driver driver = new Driver("d1", "Alice", null, null);
        when(dataRetriever.findDriverById("d1")).thenReturn(driver);
        when(revenueService.computeDriverRevenue("d1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(100000L);
        when(revenueService.computeCooperativeFee("d1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(15000L);

        ResponseEntity<?> response = driverController.getRevenue("d1", "2026-01-01", "2026-01-31");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getStatistics_shouldReturnNotFound_whenDriverMissing() {
        when(dataRetriever.findDriverById("d1")).thenReturn(null);

        ResponseEntity<?> response = driverController.getStatistics("d1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getStatistics_shouldReturnStatistics_whenDriverExists() {
        Driver driver = new Driver("d1", "Alice", null, null);
        when(dataRetriever.findDriverById("d1")).thenReturn(driver);
        when(revenueService.computeCompletedTripsCount("d1")).thenReturn(5);
        when(revenueService.computeTotalDistanceKm("d1")).thenReturn(500L);
        when(revenueService.computeAveragePricePerKm("d1")).thenReturn(120.5);

        ResponseEntity<?> response = driverController.getStatistics("d1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
