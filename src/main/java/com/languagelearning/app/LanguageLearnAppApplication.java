package com.languagelearning.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.languagelearning"})
public class LanguageLearnAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(LanguageLearnAppApplication.class, args);
	}

}
