package hei.school.kofiatrans.service;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.Trip;
import hei.school.kofiatrans.model.enums.TripStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private DataRetriever dataRetriever;

    @InjectMocks
    private RevenueService revenueService;

    private static Trip trip(String id, LocalDate date, int distanceKm, long billedAmount, TripStatus status) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setTripDate(date);
        trip.setDistanceKm(distanceKm);
        trip.setBilledAmount(billedAmount);
        trip.setStatus(status);
        return trip;
    }

    @Test
    void computeDriverRevenue_shouldSumOnlyBillableTripsWithinPeriod() {
        List<Trip> trips = List.of(
                trip("t1", LocalDate.of(2026, 1, 10), 100, 20000, TripStatus.COMPLETED),
                trip("t2", LocalDate.of(2026, 1, 20), 50, 10000, TripStatus.CANCELLED),
                trip("t3", LocalDate.of(2026, 2, 5), 80, 15000, TripStatus.COMPLETED)
        );
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(trips);

        long revenue = revenueService.computeDriverRevenue("d1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(revenue).isEqualTo(20000L);
    }

    @Test
    void computeCooperativeFee_shouldBe15PercentOfRevenue_rounded() {
        List<Trip> trips = List.of(
                trip("t1", LocalDate.of(2026, 1, 10), 100, 10000, TripStatus.COMPLETED)
        );
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(trips);

        long fee = revenueService.computeCooperativeFee("d1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(fee).isEqualTo(1500L);
    }

    @Test
    void computeAveragePricePerKm_shouldAverageOnlyBillableTrips() {
        List<Trip> trips = List.of(
                trip("t1", LocalDate.of(2026, 1, 1), 100, 20000, TripStatus.COMPLETED),
                trip("t2", LocalDate.of(2026, 1, 2), 50, 10000, TripStatus.COMPLETED),
                trip("t3", LocalDate.of(2026, 1, 3), 40, 8000, TripStatus.CANCELLED)
        );
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(trips);

        double average = revenueService.computeAveragePricePerKm("d1");

        assertThat(average).isEqualTo(200.0);
    }

    @Test
    void computeAveragePricePerKm_shouldReturnZero_whenNoTrips() {
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(List.of());

        assertThat(revenueService.computeAveragePricePerKm("d1")).isZero();
    }

    @Test
    void computeCompletedTripsCount_shouldCountOnlyBillableTrips() {
        List<Trip> trips = List.of(
                trip("t1", LocalDate.of(2026, 1, 1), 100, 20000, TripStatus.COMPLETED),
                trip("t2", LocalDate.of(2026, 1, 2), 50, 10000, TripStatus.CANCELLED)
        );
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(trips);

        assertThat(revenueService.computeCompletedTripsCount("d1")).isEqualTo(1);
    }

    @Test
    void computeTotalDistanceKm_shouldSumOnlyBillableTrips() {
        List<Trip> trips = List.of(
                trip("t1", LocalDate.of(2026, 1, 1), 100, 20000, TripStatus.COMPLETED),
                trip("t2", LocalDate.of(2026, 1, 2), 50, 10000, TripStatus.CANCELLED)
        );
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(trips);

        assertThat(revenueService.computeTotalDistanceKm("d1")).isEqualTo(100L);
    }

    @Test
    void findTopEarningDriver_shouldReturnDriverWithHighestRevenue() {
        Driver driver1 = new Driver("d1", "Alice", null, null);
        Driver driver2 = new Driver("d2", "Bob", null, null);
        when(dataRetriever.findAllDrivers()).thenReturn(List.of(driver1, driver2));
        when(dataRetriever.findTripsByDriver("d1")).thenReturn(List.of(
                trip("t1", LocalDate.of(2026, 1, 1), 100, 10000, TripStatus.COMPLETED)
        ));
        when(dataRetriever.findTripsByDriver("d2")).thenReturn(List.of(
                trip("t2", LocalDate.of(2026, 1, 1), 100, 30000, TripStatus.COMPLETED)
        ));

        Driver topDriver = revenueService.findTopEarningDriver(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(topDriver).isEqualTo(driver2);
    }

    @Test
    void findTopEarningDriver_shouldReturnNull_whenNoDrivers() {
        when(dataRetriever.findAllDrivers()).thenReturn(List.of());

        assertThat(revenueService.findTopEarningDriver(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).isNull();
    }
}
