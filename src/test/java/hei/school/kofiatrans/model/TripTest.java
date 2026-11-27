package hei.school.kofiatrans.model;

import hei.school.kofiatrans.model.enums.TripStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripTest {

    @Test
    void isBillable_shouldReturnTrue_whenStatusIsCompleted() {
        Trip trip = new Trip();
        trip.setStatus(TripStatus.COMPLETED);

        assertThat(trip.isBillable()).isTrue();
    }

    @Test
    void isBillable_shouldReturnFalse_whenStatusIsCancelled() {
        Trip trip = new Trip();
        trip.setStatus(TripStatus.CANCELLED);

        assertThat(trip.isBillable()).isFalse();
    }

    @Test
    void pricePerKm_shouldDivideBilledAmountByDistance() {
        Trip trip = new Trip();
        trip.setDistanceKm(100);
        trip.setBilledAmount(25000);

        assertThat(trip.pricePerKm()).isEqualTo(250.0);
    }

    @Test
    void pricePerKm_shouldReturnZero_whenDistanceIsZero() {
        Trip trip = new Trip();
        trip.setDistanceKm(0);
        trip.setBilledAmount(1000);

        assertThat(trip.pricePerKm()).isZero();
    }
}
