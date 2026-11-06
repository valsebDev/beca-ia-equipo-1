package com.ids.ms_tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsTicketsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsTicketsApplication.class, args);
	}

}
