package com.ids.ms_catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCatalogApplication.class, args);
	}

}
