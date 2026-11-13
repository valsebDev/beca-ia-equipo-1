package com.ids.ms_tickets.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ids.ms_tickets.config.ClientsConfig;
import com.ids.ms_tickets.config.properties.ClientsProperties;
import com.ids.ms_tickets.exception.BusinessRuleException;
import com.ids.ms_tickets.exception.DependencyUnavailableException;

@SpringJUnitConfig(RemoteClientsTest.TestConfig.class)
@TestPropertySource(properties = {"clients.connect-timeout=2s", "clients.read-timeout=1s"})
class RemoteClientsTest {

	private static final String USER_PATH = "/api/v1/users/7";
	private static final String CATEGORY_PATH = "/api/v1/categories/3";
	private static final String PRIORITY_PATH = "/api/v1/priorities/5";
	private static final String ACTIVE_USER = """
			{"id":7,"email":"agente@ids.mx","fullName":"Agente Uno","role":"AGENTE",\
			"active":true,"createdAt":"2024-01-01T10:00:00Z"}""";
	private static final String ACTIVE_CATEGORY = """
			{"id":3,"name":"Hardware","active":true}""";
	private static final String ACTIVE_PRIORITY = """
			{"id":5,"code":"ALTA","name":"Alta","level":2,"active":true,\
			"sla":{"responseHours":4,"resolutionHours":24}}""";

	private static WireMockServer server;

	@Autowired
	private RemoteUserGateway userGateway;

	@Autowired
	private RemoteCatalogGateway catalogGateway;

	@BeforeAll
	static void startServer() {
		server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		server.start();
	}

	@AfterAll
	static void stopServer() {
		server.stop();
	}

	@DynamicPropertySource
	static void providerUrls(DynamicPropertyRegistry registry) {
		registry.add("clients.users.url", server::baseUrl);
		registry.add("clients.catalog.url", server::baseUrl);
	}

	@BeforeEach
	void resetStubs() {
		server.resetAll();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	class UserGatewayTest {

		@Test
		void activeAgentIsAccepted() {
			stubJson(USER_PATH, 200, ACTIVE_USER);

			assertThatCode(() -> userGateway.ensureActiveAgent(7L)).doesNotThrowAnyException();
		}

		@Test
		void unknownUserIsRejectedAsInvalidAssignee() {
			stubStatus(USER_PATH, 404);

			assertThatThrownBy(() -> userGateway.ensureActiveAgent(7L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("ASSIGNEE_INVALID"));
		}

		@Test
		void nonAgentRoleIsRejectedAsInvalidAssignee() {
			stubJson(USER_PATH, 200, ACTIVE_USER.replace("AGENTE", "SOLICITANTE"));

			assertThatThrownBy(() -> userGateway.ensureActiveAgent(7L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("ASSIGNEE_INVALID"));
		}

		@Test
		void inactiveAgentIsRejectedAsInvalidAssignee() {
			stubJson(USER_PATH, 200, ACTIVE_USER.replace("\"active\":true", "\"active\":false"));

			assertThatThrownBy(() -> userGateway.ensureActiveAgent(7L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("ASSIGNEE_INVALID"));
		}

		@ParameterizedTest
		@ValueSource(ints = {401, 403, 500})
		void providerFailureIsReportedAsUnavailable(int status) {
			stubStatus(USER_PATH, status);

			assertThatThrownBy(() -> userGateway.ensureActiveAgent(7L))
					.isInstanceOf(DependencyUnavailableException.class);
		}

		@Test
		void slowProviderIsReportedAsUnavailable() {
			server.stubFor(get(urlEqualTo(USER_PATH)).willReturn(aResponse()
					.withStatus(200)
					.withBody(ACTIVE_USER)
					.withFixedDelay(3_000)));

			assertThatThrownBy(() -> userGateway.ensureActiveAgent(7L))
					.isInstanceOf(DependencyUnavailableException.class);
		}

		@Test
		void bearerTokenIsRelayedToProvider() {
			SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
					Jwt.withTokenValue("token-ejemplo").header("alg", "none").claim("sub", "1").build(),
					List.of()));
			stubJson(USER_PATH, 200, ACTIVE_USER);

			userGateway.ensureActiveAgent(7L);

			server.verify(getRequestedFor(urlEqualTo(USER_PATH))
					.withHeader("Authorization", equalTo("Bearer token-ejemplo")));
		}
	}

	@Nested
	class CatalogGatewayTest {

		@Test
		void activeCategoryIsAccepted() {
			stubJson(CATEGORY_PATH, 200, ACTIVE_CATEGORY);

			assertThatCode(() -> catalogGateway.ensureActiveCategory(3L)).doesNotThrowAnyException();
		}

		@Test
		void unknownCategoryIsRejected() {
			stubStatus(CATEGORY_PATH, 404);

			assertThatThrownBy(() -> catalogGateway.ensureActiveCategory(3L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("CATEGORY_INVALID"));
		}

		@Test
		void inactiveCategoryIsRejected() {
			stubJson(CATEGORY_PATH, 200, ACTIVE_CATEGORY.replace("true", "false"));

			assertThatThrownBy(() -> catalogGateway.ensureActiveCategory(3L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("CATEGORY_INVALID"));
		}

		@ParameterizedTest
		@ValueSource(ints = {401, 403, 500})
		void categoryProviderFailureIsReportedAsUnavailable(int status) {
			stubStatus(CATEGORY_PATH, status);

			assertThatThrownBy(() -> catalogGateway.ensureActiveCategory(3L))
					.isInstanceOf(DependencyUnavailableException.class);
		}

		@Test
		void activePriorityReturnsSlaSnapshot() {
			stubJson(PRIORITY_PATH, 200, ACTIVE_PRIORITY);

			PrioritySnapshot snapshot = catalogGateway.getActivePriority(5L);

			assertThat(snapshot).usingRecursiveComparison()
					.isEqualTo(new PrioritySnapshot("ALTA", 4, 24));
		}

		@Test
		void inactivePriorityIsRejected() {
			stubJson(PRIORITY_PATH, 200, ACTIVE_PRIORITY.replace("\"active\":true", "\"active\":false"));

			assertThatThrownBy(() -> catalogGateway.getActivePriority(5L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("PRIORITY_INVALID"));
		}

		@Test
		void unknownPriorityIsRejected() {
			stubStatus(PRIORITY_PATH, 404);

			assertThatThrownBy(() -> catalogGateway.getActivePriority(5L))
					.isInstanceOfSatisfying(BusinessRuleException.class,
							ex -> assertThat(ex.getCode()).isEqualTo("PRIORITY_INVALID"));
		}

		@Test
		void priorityProviderFailureIsReportedAsUnavailable() {
			stubStatus(PRIORITY_PATH, 500);

			assertThatThrownBy(() -> catalogGateway.getActivePriority(5L))
					.isInstanceOf(DependencyUnavailableException.class);
		}
	}

	private static void stubJson(String path, int status, String body) {
		server.stubFor(get(urlEqualTo(path)).willReturn(aResponse()
				.withStatus(status)
				.withHeader("Content-Type", "application/json")
				.withBody(body)));
	}

	private static void stubStatus(String path, int status) {
		server.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(status)));
	}

	@Configuration
	@EnableConfigurationProperties(ClientsProperties.class)
	@Import({ClientsConfig.class, ProviderErrors.class, BearerTokenRelayInterceptor.class,
			RemoteUserGateway.class, RemoteCatalogGateway.class})
	static class TestConfig {
	}
}
