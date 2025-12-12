package com.lovettj.surfspotsapi.config;

import com.lovettj.surfspotsapi.service.SeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    /**
     * Automatically seed the database on application startup when using the 'dev' profile.
     * This ensures the database is populated with initial data for development.
     * Explicitly excluded from 'test' profile to prevent seeding during test runs.
     */
    @Bean
    @Profile({"dev", "!test"})
    public CommandLineRunner seedData(SeedService seedService) {
        return args -> {
            logger.info("Starting data seeding for development environment...");
            try {
                seedService.seedData();
                logger.info("Data seeding completed successfully!");
            } catch (Exception e) {
                logger.error("Failed to seed data: {}", e.getMessage(), e);
            }
        };
    }
}

