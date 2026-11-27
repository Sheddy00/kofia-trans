package hei.school.kofiatrans.config;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.db.DbConnection;
import hei.school.kofiatrans.db.UserRepository;
import hei.school.kofiatrans.service.AuthService;
import hei.school.kofiatrans.service.RevenueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@Configuration
public class BeanConfiguration {

    @Bean
    public DbConnection dbConnection(DataSource dataSource) {
        return new DbConnection(dataSource);
    }

    @Bean
    public DataRetriever dataRetriever(DbConnection dbConnection) {
        return new DataRetriever(dbConnection);
    }

    @Bean
    public RevenueService revenueService(DataRetriever dataRetriever) {
        return new RevenueService(dataRetriever);
    }

    @Bean
    public UserRepository userRepository(DbConnection dbConnection) {
        return new UserRepository(dbConnection);
    }

    @Bean
    public AuthService authService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new AuthService(userRepository, passwordEncoder);
    }

    @Bean
    public UserBootstrap userBootstrap(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${app.bootstrap.admin.username:}") String adminUsername,
                                       @Value("${app.bootstrap.admin.password:}") String adminPassword,
                                       @Value("${app.bootstrap.demo.username:}") String demoUsername,
                                       @Value("${app.bootstrap.demo.password:}") String demoPassword) {
        return new UserBootstrap(userRepository, passwordEncoder,
                new UserBootstrap.Account(adminUsername, adminPassword),
                new UserBootstrap.Account(demoUsername, demoPassword));
    }
}
