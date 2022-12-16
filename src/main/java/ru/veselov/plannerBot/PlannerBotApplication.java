package ru.veselov.plannerBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlannerBotApplication {


	public static void main(String[] args) {

		SpringApplication.run(PlannerBotApplication.class, args);
	}

}
