package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.TripStatusUpdateRequestDto;
import hei.school.kofiatrans.controller.dto.TripUpsertRequestDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.Trip;
import hei.school.kofiatrans.model.Vehicle;
import hei.school.kofiatrans.model.enums.TripStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    @Mock
    private DataRetriever dataRetriever;

    @InjectMocks
    private TripController tripController;

    private static Trip trip(String id) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setDriver(new Driver("d1", "Alice", null, null));
        trip.setVehicle(new Vehicle("v1", "AA-123-BB", null, 1.0));
        trip.setTripDate(LocalDate.of(2026, 1, 15));
        trip.setDepartureCity("Antananarivo");
        trip.setArrivalCity("Toamasina");
        trip.setDistanceKm(300);
        trip.setBilledAmount(50000);
        trip.setStatus(TripStatus.COMPLETED);
        return trip;
    }

    @Test
    void findTrips_shouldReturnAllTrips_whenNoFilterProvided() {
        when(dataRetriever.findAllTrips()).thenReturn(List.of(trip("t1")));

        var result = tripController.findTrips(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("t1");
    }

    @Test
    void findTrips_shouldFilterByDriverId() {
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(List.of(trip("t1")));

        var result = tripController.findTrips("d1", null, null);

        assertThat(result).hasSize(1);
        verify(dataRetriever, never()).findAllTrips();
    }

    @Test
    void findTrips_shouldFilterByPeriod() {
        when(dataRetriever.findTripsByPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(trip("t1")));

        var result = tripController.findTrips(null, "2026-01-01", "2026-01-31");

        assertThat(result).hasSize(1);
    }

    @Test
    void findTrips_shouldThrow_whenOnlyFromProvided() {
        assertThrows(ClientException.class, () -> tripController.findTrips(null, "2026-01-01", null));
    }

    @Test
    void findTrips_shouldThrow_whenDateIsInvalid() {
        assertThrows(ClientException.class, () -> tripController.findTrips(null, "not-a-date", "2026-01-31"));
    }

    @Test
    void upsertTrip_shouldThrow_whenFieldsMissing() {
        TripUpsertRequestDto request = new TripUpsertRequestDto(null, "v1", LocalDate.now(), "A", "B", 10, 1000L);

        assertThrows(ClientException.class, () -> tripController.upsertTrip("t1", request));
    }

    @Test
    void upsertTrip_shouldThrow_whenDistanceNotPositive() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 0, 1000L);

        assertThrows(ClientException.class, () -> tripController.upsertTrip("t1", request));
    }

    @Test
    void upsertTrip_shouldThrow_whenBilledAmountNegative() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 10, -1L);

        assertThrows(ClientException.class, () -> tripController.upsertTrip("t1", request));
    }

    @Test
    void upsertTrip_shouldThrow_whenDriverNotFound() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 10, 1000L);
        when(dataRetriever.findDriverById("d1")).thenReturn(null);

        assertThrows(ClientException.class, () -> tripController.upsertTrip("t1", request));
    }

    @Test
    void upsertTrip_shouldThrow_whenVehicleNotFound() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 10, 1000L);
        when(dataRetriever.findDriverById("d1")).thenReturn(new Driver("d1", "Alice", null, null));
        when(dataRetriever.findVehicleById("v1")).thenReturn(null);

        assertThrows(ClientException.class, () -> tripController.upsertTrip("t1", request));
    }

    @Test
    void upsertTrip_shouldReturnCreated_whenTripDidNotExist() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 10, 1000L);
        when(dataRetriever.findDriverById("d1")).thenReturn(new Driver("d1", "Alice", null, null));
        when(dataRetriever.findVehicleById("v1")).thenReturn(new Vehicle("v1", "AA-123-BB", null, 1.0));
        when(dataRetriever.findTripById("t1")).thenReturn(null);
        when(dataRetriever.saveTrip(any(Trip.class))).thenReturn(trip("t1"));

        ResponseEntity<?> response = tripController.upsertTrip("t1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void upsertTrip_shouldReturnOk_whenTripAlreadyExisted() {
        TripUpsertRequestDto request = new TripUpsertRequestDto("d1", "v1", LocalDate.now(), "A", "B", 10, 1000L);
        when(dataRetriever.findDriverById("d1")).thenReturn(new Driver("d1", "Alice", null, null));
        when(dataRetriever.findVehicleById("v1")).thenReturn(new Vehicle("v1", "AA-123-BB", null, 1.0));
        when(dataRetriever.findTripById("t1")).thenReturn(trip("t1"));
        when(dataRetriever.saveTrip(any(Trip.class))).thenReturn(trip("t1"));

        ResponseEntity<?> response = tripController.upsertTrip("t1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateTripStatus_shouldThrow_whenStatusMissing() {
        assertThrows(ClientException.class,
                () -> tripController.updateTripStatus("t1", new TripStatusUpdateRequestDto(null)));
    }

    @Test
    void updateTripStatus_shouldThrow_whenStatusInvalid() {
        assertThrows(ClientException.class,
                () -> tripController.updateTripStatus("t1", new TripStatusUpdateRequestDto("UNKNOWN")));
    }

    @Test
    void updateTripStatus_shouldReturnNotFound_whenTripMissing() {
        when(dataRetriever.findTripById("t1")).thenReturn(null);

        ResponseEntity<?> response = tripController.updateTripStatus("t1", new TripStatusUpdateRequestDto("CANCELLED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateTripStatus_shouldReturnUpdatedTrip() {
        when(dataRetriever.findTripById("t1")).thenReturn(trip("t1"));
        Trip updated = trip("t1");
        updated.setStatus(TripStatus.CANCELLED);
        when(dataRetriever.updateTripStatus("t1", TripStatus.CANCELLED)).thenReturn(updated);

        ResponseEntity<?> response = tripController.updateTripStatus("t1", new TripStatusUpdateRequestDto("CANCELLED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
