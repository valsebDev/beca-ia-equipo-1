package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.dto.response.SlaResponse;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.PriorityCode;
import com.ids.ms_catalog.security.AdminGuard;
import com.ids.ms_catalog.service.PriorityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ids.ms_catalog.security.SecurityTestSupport.jwtWithRole;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriorityController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class PriorityControllerSecurityTest {

	private static final String BASE_PATH = "/api/v1/priorities";
	private static final String SLA_PATH = "/api/v1/priorities/2/sla";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String CODE_PATH = "$.code";
	private static final String CREATE_BODY = "{\"code\":\"MEDIA\",\"name\":\"Media\",\"level\":3,"
			+ "\"sla\":{\"responseHours\":4,\"resolutionHours\":24}}";
	private static final String SLA_BODY = "{\"responseHours\":2,\"resolutionHours\":8}";

	private final MockMvc mockMvc;

	@MockitoBean
	private AdminGuard adminGuard;

	@MockitoBean
	private PriorityService priorityService;

	@Autowired
	PriorityControllerSecurityTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@ParameterizedTest
	@CsvSource({
			"GET,/api/v1/priorities",
			"GET,/api/v1/priorities/1",
			"POST,/api/v1/priorities",
			"PUT,/api/v1/priorities/2/sla"
	})
	void todaRutaProtegidaSinTokenDevuelve401(String method, String path) throws Exception {
		mockMvc.perform(request(HttpMethod.valueOf(method), path).contentType(JSON).content(CREATE_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath(CODE_PATH).value("UNAUTHORIZED"));
		verifyNoInteractions(priorityService);
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void lecturaPermitidaParaTodosLosRoles(String role) throws Exception {
		when(priorityService.findAll()).thenReturn(List.of());

		mockMvc.perform(get(BASE_PATH).with(jwtWithRole(role)))
				.andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE", "ADMIN"})
	void consultaPrioridadPorIdPermitidaParaTodosLosRoles(String role) throws Exception {
		when(priorityService.findById(1L)).thenReturn(
				new PriorityResponse(1L, PriorityCode.MEDIA, "Media", 3, true, new SlaResponse(4, 24)));

		mockMvc.perform(get(BASE_PATH + "/1").with(jwtWithRole(role)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("MEDIA"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void crearPrioridadRequiereAdmin(String role) throws Exception {
		mockMvc.perform(post(BASE_PATH).with(jwtWithRole(role)).contentType(JSON).content(CREATE_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath(CODE_PATH).value("FORBIDDEN"));
		verifyNoInteractions(priorityService);
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void actualizarSlaRequiereAdmin(String role) throws Exception {
		mockMvc.perform(put(SLA_PATH).with(jwtWithRole(role)).contentType(JSON).content(SLA_BODY))
				.andExpect(status().isForbidden());
		verifyNoInteractions(priorityService);
	}

	@Test
	void adminPuedeActualizarSlaYLlegaAlService() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenThrow(
				new NotFoundException("PRIORITY_NOT_FOUND", "No existe"));

		mockMvc.perform(put(SLA_PATH).with(jwtWithRole("ADMIN")).contentType(JSON).content(SLA_BODY))
				.andExpect(status().isNotFound());
	}
}
