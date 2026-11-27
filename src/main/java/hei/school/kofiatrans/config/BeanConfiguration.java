package hei.school.kofiatrans.config;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.db.DbConnection;
import hei.school.kofiatrans.service.RevenueService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
