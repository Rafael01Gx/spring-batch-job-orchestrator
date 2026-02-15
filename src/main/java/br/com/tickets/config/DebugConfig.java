package br.com.tickets.config;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class DebugConfig {

    @Bean
    CommandLineRunner debugJobRepository(JobRepository jobRepository, DataSource dataSource) {
        return args -> {
            System.out.println("=== DEBUG INFO ===");
            System.out.println("JobRepository class: " + jobRepository.getClass().getName());

            try (Connection conn = dataSource.getConnection()) {
                System.out.println("DataSource URL: " + conn.getMetaData().getURL());
                System.out.println("Database: " + conn.getMetaData().getDatabaseProductName());
            }
            System.out.println("==================");
        };
    }
}