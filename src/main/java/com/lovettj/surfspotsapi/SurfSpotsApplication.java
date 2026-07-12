package com.lovettj.surfspotsapi;

import com.lovettj.surfspotsapi.config.AppProperties;
import com.lovettj.surfspotsapi.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
@EnableCaching
@EnableAsync
@EnableScheduling
public class SurfSpotsApplication {

	public static void main(String[] args) {
		DotEnvLoader.load(); // Load .env from project root so ${VAR} in application.yml resolve
		SpringApplication.run(SurfSpotsApplication.class, args);
	}
}
