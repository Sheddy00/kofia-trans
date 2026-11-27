package hei.school.kofiatrans.db;

import hei.school.kofiatrans.exception.ServerException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.Trip;
import hei.school.kofiatrans.model.Vehicle;
import hei.school.kofiatrans.model.enums.LicenseCategory;
import hei.school.kofiatrans.model.enums.TripStatus;
import hei.school.kofiatrans.model.enums.VehicleType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    /** Loads each trip with its driver and vehicle in a single query (no N+1 lookups). */
    private static final String TRIP_SELECT = "SELECT t.id, t.trip_date, t.departure_city, t.arrival_city, "
            + "t.distance_km, t.billed_amount, t.status, "
            + "d.id AS driver_id, d.name AS driver_name, d.license_category AS driver_license_category, "
            + "d.affiliation_date AS driver_affiliation_date, "
            + "v.id AS vehicle_id, v.plate_number AS vehicle_plate_number, v.type AS vehicle_type, "
            + "v.capacity_tons AS vehicle_capacity_tons "
            + "FROM trip t "
            + "JOIN driver d ON d.id = t.driver_id "
            + "JOIN vehicle v ON v.id = t.vehicle_id";

    private final DbConnection dbConnection;

    public DataRetriever(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Driver findDriverById(String id) {
        String sql = "SELECT id, name, license_category, affiliation_date FROM driver WHERE id = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapDriver(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public List<Driver> findAllDrivers() {
        String sql = "SELECT id, name, license_category, affiliation_date FROM driver";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Driver> drivers = new ArrayList<>();
            while (resultSet.next()) {
                drivers.add(mapDriver(resultSet));
            }
            return drivers;
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public Vehicle findVehicleById(String id) {
        String sql = "SELECT id, plate_number, type, capacity_tons FROM vehicle WHERE id = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapVehicle(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public Trip findTripById(String id) {
        String sql = TRIP_SELECT + " WHERE t.id = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTrip(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public List<Trip> findTripsByDriver(String driverId) {
        String sql = TRIP_SELECT + " WHERE t.driver_id = ? ORDER BY t.trip_date";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, driverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Trip> trips = new ArrayList<>();
                while (resultSet.next()) {
                    trips.add(mapTrip(resultSet));
                }
                return trips;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public List<Trip> findTripsByPeriod(LocalDate from, LocalDate to) {
        String sql = TRIP_SELECT + " WHERE t.trip_date BETWEEN ? AND ? ORDER BY t.trip_date";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Trip> trips = new ArrayList<>();
                while (resultSet.next()) {
                    trips.add(mapTrip(resultSet));
                }
                return trips;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public List<Trip> findAllTrips() {
        String sql = TRIP_SELECT + " ORDER BY t.trip_date";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Trip> trips = new ArrayList<>();
            while (resultSet.next()) {
                trips.add(mapTrip(resultSet));
            }
            return trips;
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public Trip saveTrip(Trip trip) {
        String sql = "INSERT INTO trip (id, driver_id, vehicle_id, trip_date, departure_city, arrival_city, "
                + "distance_km, billed_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (id) DO UPDATE SET "
                + "driver_id = EXCLUDED.driver_id, vehicle_id = EXCLUDED.vehicle_id, "
                + "trip_date = EXCLUDED.trip_date, departure_city = EXCLUDED.departure_city, "
                + "arrival_city = EXCLUDED.arrival_city, distance_km = EXCLUDED.distance_km, "
                + "billed_amount = EXCLUDED.billed_amount, status = EXCLUDED.status";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trip.getId());
            statement.setString(2, trip.getDriver().getId());
            statement.setString(3, trip.getVehicle().getId());
            statement.setDate(4, Date.valueOf(trip.getTripDate()));
            statement.setString(5, trip.getDepartureCity());
            statement.setString(6, trip.getArrivalCity());
            statement.setInt(7, trip.getDistanceKm());
            statement.setLong(8, trip.getBilledAmount());
            statement.setString(9, trip.getStatus().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ServerException(e);
        }
        return findTripById(trip.getId());
    }

    public Trip updateTripStatus(String tripId, TripStatus status) {
        String sql = "UPDATE trip SET status = ? WHERE id = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, tripId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                return null;
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
        return findTripById(tripId);
    }

    private Driver mapDriver(ResultSet resultSet) throws SQLException {
        return new Driver(
                resultSet.getString("id"),
                resultSet.getString("name"),
                LicenseCategory.valueOf(resultSet.getString("license_category")),
                resultSet.getDate("affiliation_date").toLocalDate()
        );
    }

    private Vehicle mapVehicle(ResultSet resultSet) throws SQLException {
        return new Vehicle(
                resultSet.getString("id"),
                resultSet.getString("plate_number"),
                VehicleType.valueOf(resultSet.getString("type")),
                resultSet.getDouble("capacity_tons")
        );
    }

    private Trip mapTrip(ResultSet resultSet) throws SQLException {
        Driver driver = new Driver(
                resultSet.getString("driver_id"),
                resultSet.getString("driver_name"),
                LicenseCategory.valueOf(resultSet.getString("driver_license_category")),
                resultSet.getDate("driver_affiliation_date").toLocalDate()
        );
        Vehicle vehicle = new Vehicle(
                resultSet.getString("vehicle_id"),
                resultSet.getString("vehicle_plate_number"),
                VehicleType.valueOf(resultSet.getString("vehicle_type")),
                resultSet.getDouble("vehicle_capacity_tons")
        );
        return new Trip(
                resultSet.getString("id"),
                driver,
                vehicle,
                resultSet.getDate("trip_date").toLocalDate(),
                resultSet.getString("departure_city"),
                resultSet.getString("arrival_city"),
                resultSet.getInt("distance_km"),
                resultSet.getLong("billed_amount"),
                TripStatus.valueOf(resultSet.getString("status"))
        );
    }
}
