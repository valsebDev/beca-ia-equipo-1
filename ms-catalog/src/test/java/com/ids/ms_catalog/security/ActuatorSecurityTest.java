package com.ids.ms_catalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.ids.ms_catalog.security.SecurityTestSupport.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ActuatorSecurityTest {

	private static final String HEALTH_PATH = "/actuator/health";
	private static final String ENV_PATH = "/actuator/env";

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	private final MockMvc mockMvc;

	@Autowired
	ActuatorSecurityTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void healthEsPublico() throws Exception {
		mockMvc.perform(get(HEALTH_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void otrosEndpointsDeActuatorRequierenToken() throws Exception {
		mockMvc.perform(get(ENV_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}

	@Test
	void otrosEndpointsDeActuatorNoEstanExpuestosAunConToken() throws Exception {
		mockMvc.perform(get(ENV_PATH).with(jwtWithRole("ADMIN")))
				.andExpect(status().isNotFound());
	}
}
