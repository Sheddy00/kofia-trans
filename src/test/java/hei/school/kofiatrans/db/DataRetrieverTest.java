package hei.school.kofiatrans.db;

import hei.school.kofiatrans.exception.ConflictException;
import hei.school.kofiatrans.model.Trip;
import hei.school.kofiatrans.model.enums.LicenseCategory;
import hei.school.kofiatrans.model.enums.TripStatus;
import hei.school.kofiatrans.model.enums.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetrieverTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    @Mock
    private ResultSet resultSet;

    private DataRetriever dataRetriever;

    @BeforeEach
    void setUp() throws Exception {
        dataRetriever = new DataRetriever(new DbConnection(dataSource));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void findAllTrips_shouldLoadTripsWithDriverAndVehicleInSingleQuery() throws Exception {
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("id")).thenReturn("t-001", "t-002");
        when(resultSet.getDate("trip_date")).thenReturn(Date.valueOf("2026-04-02"), Date.valueOf("2026-04-07"));
        when(resultSet.getString("departure_city")).thenReturn("Antananarivo");
        when(resultSet.getString("arrival_city")).thenReturn("Toamasina");
        when(resultSet.getInt("distance_km")).thenReturn(350);
        when(resultSet.getLong("billed_amount")).thenReturn(2_800_000L);
        when(resultSet.getString("status")).thenReturn("COMPLETED");
        when(resultSet.getString("driver_id")).thenReturn("d-001");
        when(resultSet.getString("driver_name")).thenReturn("Rakoto Jean");
        when(resultSet.getString("driver_license_category")).thenReturn("C");
        when(resultSet.getDate("driver_affiliation_date")).thenReturn(Date.valueOf("2022-01-15"));
        when(resultSet.getString("vehicle_id")).thenReturn("v-001");
        when(resultSet.getString("vehicle_plate_number")).thenReturn("TAA-1001");
        when(resultSet.getString("vehicle_type")).thenReturn("CAMION");
        when(resultSet.getDouble("vehicle_capacity_tons")).thenReturn(12.5);

        List<Trip> trips = dataRetriever.findAllTrips();

        assertThat(trips).hasSize(2);
        Trip first = trips.getFirst();
        assertThat(first.getId()).isEqualTo("t-001");
        assertThat(first.getTripDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(first.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(first.getDriver().getName()).isEqualTo("Rakoto Jean");
        assertThat(first.getDriver().getLicenseCategory()).isEqualTo(LicenseCategory.C);
        assertThat(first.getVehicle().getPlateNumber()).isEqualTo("TAA-1001");
        assertThat(first.getVehicle().getType()).isEqualTo(VehicleType.CAMION);

        // One connection and one statement for the whole list: no per-trip driver/vehicle lookups.
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).prepareStatement(anyString());
    }

    @Test
    void findTripsByDriver_shouldFilterOnJoinedTripTable() throws Exception {
        when(resultSet.next()).thenReturn(false);

        assertThat(dataRetriever.findTripsByDriver("d-001")).isEmpty();

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains("JOIN driver d ON d.id = t.driver_id"));
        verify(statement).setString(1, "d-001");
    }

    @Test
    void findTripsPage_shouldBindFiltersThenLimitAndOffset() throws Exception {
        when(resultSet.next()).thenReturn(false);

        dataRetriever.findTripsPage("d-001", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), 20, 40);

        verify(connection).prepareStatement(org.mockito.ArgumentMatchers.contains(
                "WHERE t.driver_id = ? AND t.trip_date >= ? AND t.trip_date <= ? ORDER BY t.trip_date, t.id LIMIT ? OFFSET ?"));
        verify(statement).setString(1, "d-001");
        verify(statement).setDate(2, Date.valueOf("2026-04-01"));
        verify(statement).setDate(3, Date.valueOf("2026-04-30"));
        verify(statement).setInt(4, 20);
        verify(statement).setInt(5, 40);
    }

    @Test
    void deleteDriver_shouldRefuse_whenTripsReferenceIt() throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(4);

        assertThatThrownBy(() -> dataRetriever.deleteDriver("d-001"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("4 course(s)");
        verify(statement, times(0)).executeUpdate();
    }
}
