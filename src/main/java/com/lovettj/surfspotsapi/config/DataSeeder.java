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
     * One-time load of reference data from JSON when the database is empty (no continents yet).
     * Production data changes belong in migrations, not here. Excluded from the {@code test} profile.
     */
    @Bean
    @Profile({"dev", "prod", "!test"})
    public CommandLineRunner seedData(SeedService seedService) {
        return args -> {
            logger.info("Starting data seeding...");
            try {
                seedService.seedData();
                logger.info("Data seeding completed successfully!");
            } catch (Exception e) {
                logger.error("Failed to seed data: {}", e.getMessage(), e);
            }
        };
    }
}

