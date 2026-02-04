package com.lovettj.surfspotsapi;

import com.lovettj.surfspotsapi.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SurfSpotsApplication {

	public static void main(String[] args) {
		DotEnvLoader.load(); // Load .env from project root so ${VAR} in application.yml resolve
		SpringApplication.run(SurfSpotsApplication.class, args);
	}
}
