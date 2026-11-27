package hei.school.kofiatrans.db;

import hei.school.kofiatrans.exception.ServerException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hands out connections from the pooled {@link DataSource} (HikariCP) configured by Spring Boot
 * from the {@code spring.datasource.*} properties, instead of opening a new physical connection per query.
 */
public class DbConnection {
    private final DataSource dataSource;

    public DbConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }
}
