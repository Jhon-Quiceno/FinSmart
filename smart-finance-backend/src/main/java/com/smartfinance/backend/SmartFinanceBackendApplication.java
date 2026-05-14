package com.smartfinance.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SmartFinanceBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartFinanceBackendApplication.class, args);
	}

}
