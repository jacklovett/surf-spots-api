package com.lovettj.surfspotsapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.lovettj.surfspotsapi.service.SeedService;

@SpringBootApplication
public class SurfSpotsApplication {

	@Autowired
	private SeedService seedService;

	public static void main(String[] args) {
		SpringApplication.run(SurfSpotsApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		seedService.seedData();
	}

}
