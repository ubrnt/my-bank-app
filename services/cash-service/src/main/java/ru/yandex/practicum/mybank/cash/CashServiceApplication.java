package ru.yandex.practicum.mybank.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class CashServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashServiceApplication.class, args);
	}
}
