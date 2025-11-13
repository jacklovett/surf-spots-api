package com.lovettj.surfspotsapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import com.lovettj.surfspotsapi.service.SeedService;

@SpringBootApplication
@EnableCaching
public class SurfSpotsApplication implements CommandLineRunner {

	private final SeedService seedService;

	// Constructor injection without @Autowired
	public SurfSpotsApplication(SeedService seedService) {
		this.seedService = seedService;
	}

	public static void main(String[] args) {
		SpringApplication.run(SurfSpotsApplication.class, args);
	}

	// CommandLineRunner method to run after the application starts
	@Override
	public void run(String... args) throws Exception {
		seedService.seedData();
	}
}
