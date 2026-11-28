package hei.school.kofiatrans.db;

import hei.school.kofiatrans.exception.ConflictException;
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

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

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

    public List<Vehicle> findAllVehicles() {
        String sql = "SELECT id, plate_number, type, capacity_tons FROM vehicle ORDER BY plate_number";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Vehicle> vehicles = new ArrayList<>();
            while (resultSet.next()) {
                vehicles.add(mapVehicle(resultSet));
            }
            return vehicles;
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    public Driver saveDriver(Driver driver) {
        String sql = "INSERT INTO driver (id, name, license_category, affiliation_date) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, "
                + "license_category = EXCLUDED.license_category, affiliation_date = EXCLUDED.affiliation_date";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, driver.getId());
            statement.setString(2, driver.getName());
            statement.setString(3, driver.getLicenseCategory().name());
            statement.setDate(4, Date.valueOf(driver.getAffiliationDate()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ServerException(e);
        }
        return findDriverById(driver.getId());
    }

    /** Deletes the driver; returns false when it does not exist. Refused while trips still reference it. */
    public boolean deleteDriver(String id) {
        int trips = countReferences("SELECT COUNT(*) FROM trip WHERE driver_id = ?", id);
        if (trips > 0) {
            throw new ConflictException("Impossible de supprimer ce chauffeur : " + trips + " course(s) y sont rattachées");
        }
        return deleteById("DELETE FROM driver WHERE id = ?", id);
    }

    public Vehicle saveVehicle(Vehicle vehicle) {
        String sql = "INSERT INTO vehicle (id, plate_number, type, capacity_tons) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (id) DO UPDATE SET plate_number = EXCLUDED.plate_number, "
                + "type = EXCLUDED.type, capacity_tons = EXCLUDED.capacity_tons";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehicle.getId());
            statement.setString(2, vehicle.getPlateNumber());
            statement.setString(3, vehicle.getType().name());
            statement.setDouble(4, vehicle.getCapacityTons());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ConflictException("La plaque " + vehicle.getPlateNumber() + " est déjà utilisée par un autre véhicule");
            }
            throw new ServerException(e);
        }
        return findVehicleById(vehicle.getId());
    }

    /** Deletes the vehicle; returns false when it does not exist. Refused while trips still reference it. */
    public boolean deleteVehicle(String id) {
        int trips = countReferences("SELECT COUNT(*) FROM trip WHERE vehicle_id = ?", id);
        if (trips > 0) {
            throw new ConflictException("Impossible de supprimer ce véhicule : " + trips + " course(s) y sont rattachées");
        }
        return deleteById("DELETE FROM vehicle WHERE id = ?", id);
    }

    public boolean deleteTrip(String id) {
        return deleteById("DELETE FROM trip WHERE id = ?", id);
    }

    /** One page of trips matching the optional filters, oldest first (same order as the unpaginated list). */
    public List<Trip> findTripsPage(String driverId, LocalDate from, LocalDate to, int limit, int offset) {
        TripFilter filter = TripFilter.of(driverId, from, to);
        String sql = TRIP_SELECT + filter.where() + " ORDER BY t.trip_date, t.id LIMIT ? OFFSET ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = filter.bind(statement);
            statement.setInt(index, limit);
            statement.setInt(index + 1, offset);
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

    public long countTrips(String driverId, LocalDate from, LocalDate to) {
        TripFilter filter = TripFilter.of(driverId, from, to);
        String sql = "SELECT COUNT(*) FROM trip t" + filter.where();
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    private int countReferences(String sql, String id) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    private boolean deleteById(String sql, String id) {
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                throw new ConflictException("Suppression impossible : des courses y sont encore rattachées");
            }
            throw new ServerException(e);
        }
    }

    /** Optional trip filters turned into a parameterised WHERE clause (values are always bound, never concatenated). */
    private record TripFilter(String driverId, LocalDate from, LocalDate to) {
        static TripFilter of(String driverId, LocalDate from, LocalDate to) {
            return new TripFilter(driverId, from, to);
        }

        String where() {
            List<String> conditions = new ArrayList<>();
            if (driverId != null) conditions.add("t.driver_id = ?");
            if (from != null) conditions.add("t.trip_date >= ?");
            if (to != null) conditions.add("t.trip_date <= ?");
            return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        }

        /** Binds the filter values in order and returns the next free parameter index. */
        int bind(PreparedStatement statement) throws SQLException {
            int index = 1;
            if (driverId != null) statement.setString(index++, driverId);
            if (from != null) statement.setDate(index++, Date.valueOf(from));
            if (to != null) statement.setDate(index++, Date.valueOf(to));
            return index;
        }
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
