package com.brianna.jobsearch.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@Profile("demo")
public class DemoDataConfig {

    /**
     * Rebuild the synthetic demo dataset on every demo startup.
     * The demo profile points at demo-jobsearch.db, never jobsearch.db.
     */
    @Bean
    @Order(100)
    ApplicationRunner demoDataLoader(DataSource dataSource) {
        return args -> {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("demo-data.sql"));
            populator.execute(dataSource);
        };
    }
}
