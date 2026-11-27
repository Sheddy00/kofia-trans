package hei.school.kofiatrans.db;

import hei.school.kofiatrans.exception.ServerException;
import hei.school.kofiatrans.model.AppUser;
import hei.school.kofiatrans.model.enums.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepository {
    private final DbConnection dbConnection;

    public UserRepository(DbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT username, password_hash, role FROM app_user WHERE username = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AppUser(
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        Role.valueOf(resultSet.getString("role"))
                ));
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    /** Inserts the user unless the username is already taken; returns true when a row was created. */
    public boolean createIfAbsent(AppUser user) {
        String sql = "INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, ?) "
                + "ON CONFLICT (username) DO NOTHING";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username());
            statement.setString(2, user.passwordHash());
            statement.setString(3, user.role().name());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }
}
