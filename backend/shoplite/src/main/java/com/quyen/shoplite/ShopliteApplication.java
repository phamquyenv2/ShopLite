package com.quyen.shoplite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShopliteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopliteApplication.class, args);
	}

}
