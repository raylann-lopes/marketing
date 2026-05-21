package com.north.producoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProducesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProducesApplication.class, args);
	}

}
