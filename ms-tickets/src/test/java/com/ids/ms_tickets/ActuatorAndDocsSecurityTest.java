package com.ids.ms_tickets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.ids.ms_tickets.security.JwtTestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ActuatorAndDocsSecurityTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void health_isPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void apiDocs_arePublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void unexposedActuatorEndpoint_returns404ForAuthenticatedCaller() throws Exception {
		String token = JwtTestTokens.token(claims -> { });

		mockMvc.perform(get("/actuator/env")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void unexposedActuatorEndpoint_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
	}
}
