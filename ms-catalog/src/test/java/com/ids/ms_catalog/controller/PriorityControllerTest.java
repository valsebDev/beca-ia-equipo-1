package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.dto.response.SlaResponse;
import com.ids.ms_catalog.exception.DomainValidationException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.PriorityCode;
import com.ids.ms_catalog.service.PriorityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriorityController.class)
class PriorityControllerTest {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String SLA_PATH = "/api/v1/priorities/2/sla";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PriorityService priorityService;

	@Test
	void updateSlaDevuelve200ConLasNuevasHoras() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenReturn(
				new PriorityResponse(2L, PriorityCode.ALTA, "Alta", 2, true, new SlaResponse(2, 8)));

		mockMvc.perform(put(SLA_PATH).contentType(JSON).content("{\"responseHours\":2,\"resolutionHours\":8}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sla.responseHours").value(2))
				.andExpect(jsonPath("$.sla.resolutionHours").value(8));
	}

	@Test
	void updateSlaDevuelve400ConCampoInvalidoCuandoResolucionEsMenor() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenThrow(
				new DomainValidationException("resolutionHours", "debe ser mayor o igual que responseHours"));

		mockMvc.perform(put(SLA_PATH).contentType(JSON).content("{\"responseHours\":10,\"resolutionHours\":5}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("resolutionHours"));
	}

	@Test
	void updateSlaDevuelve400CuandoLasHorasSonCero() throws Exception {
		mockMvc.perform(put(SLA_PATH).contentType(JSON).content("{\"responseHours\":0,\"resolutionHours\":5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("responseHours"));
	}

	@Test
	void updateSlaDevuelve404ConProblemDetailCuandoLaPrioridadNoExiste() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenThrow(
				new NotFoundException("PRIORITY_NOT_FOUND", "No existe una prioridad con id 999"));

		mockMvc.perform(put("/api/v1/priorities/999/sla").contentType(JSON)
						.content("{\"responseHours\":2,\"resolutionHours\":8}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRIORITY_NOT_FOUND"));
	}

	@Test
	void createDevuelve400MalformedCuandoElCodigoEstaFueraDelDominio() throws Exception {
		String body = "{\"code\":\"URGENTE\",\"name\":\"Urgente\",\"level\":5,"
				+ "\"sla\":{\"responseHours\":1,\"resolutionHours\":2}}";

		mockMvc.perform(post("/api/v1/priorities").contentType(JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}
}
