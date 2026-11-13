package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.dto.response.SlaResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.DomainValidationException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.PriorityCode;
import com.ids.ms_catalog.security.AdminGuard;
import com.ids.ms_catalog.service.PriorityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ids.ms_catalog.security.SecurityTestSupport.admin;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriorityController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class PriorityControllerTest {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String BASE_PATH = "/api/v1/priorities";
	private static final String SLA_PATH = "/api/v1/priorities/2/sla";
	private static final String CODE_PATH = "$.code";
	private static final String ALTA_NAME = "Alta";

	private final MockMvc mockMvc;

	@MockitoBean
	private AdminGuard adminGuard;

	@MockitoBean
	private PriorityService priorityService;

	@Autowired
	PriorityControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void updateSlaDevuelve200ConLasNuevasHoras() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenReturn(
				new PriorityResponse(2L, PriorityCode.ALTA, ALTA_NAME, 2, true, new SlaResponse(2, 8)));

		mockMvc.perform(put(SLA_PATH).with(admin()).contentType(JSON).content("{\"responseHours\":2,\"resolutionHours\":8}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sla.responseHours").value(2))
				.andExpect(jsonPath("$.sla.resolutionHours").value(8));
		verify(priorityService).updateSla(2L, new SlaRequest(2, 8));
	}

	@Test
	void updateSlaDevuelve400ConCampoInvalidoCuandoResolucionEsMenor() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenThrow(
				new DomainValidationException("resolutionHours", "debe ser mayor o igual que responseHours"));

		mockMvc.perform(put(SLA_PATH).with(admin()).contentType(JSON).content("{\"responseHours\":10,\"resolutionHours\":5}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath(CODE_PATH).value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("resolutionHours"));
		verify(priorityService).updateSla(anyLong(), any());
	}

	@Test
	void updateSlaDevuelve400CuandoLasHorasSonCero() throws Exception {
		mockMvc.perform(put(SLA_PATH).with(admin()).contentType(JSON).content("{\"responseHours\":0,\"resolutionHours\":5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("responseHours"));
		verify(priorityService, never()).updateSla(anyLong(), any());
	}

	@Test
	void updateSlaDevuelve404ConProblemDetailCuandoLaPrioridadNoExiste() throws Exception {
		when(priorityService.updateSla(anyLong(), any())).thenThrow(
				new NotFoundException("PRIORITY_NOT_FOUND", "No existe una prioridad con id 999"));

		mockMvc.perform(put("/api/v1/priorities/999/sla").with(admin()).contentType(JSON)
						.content("{\"responseHours\":2,\"resolutionHours\":8}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(CODE_PATH).value("PRIORITY_NOT_FOUND"));
		verify(priorityService).updateSla(999L, new SlaRequest(2, 8));
	}

	@Test
	void createDevuelve400MalformedCuandoElCodigoEstaFueraDelDominio() throws Exception {
		String body = "{\"code\":\"URGENTE\",\"name\":\"Urgente\",\"level\":5,"
				+ "\"sla\":{\"responseHours\":1,\"resolutionHours\":2}}";

		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE_PATH).value("MALFORMED_REQUEST"));
		verify(priorityService, never()).create(any());
	}

	@Test
	void listDevuelve200ConPrioridadesOrdenadas() throws Exception {
		when(priorityService.findAll()).thenReturn(List.of(
				new PriorityResponse(2L, PriorityCode.ALTA, ALTA_NAME, 2, true, new SlaResponse(2, 8))));

		mockMvc.perform(get(BASE_PATH).with(admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("ALTA"));
		verify(priorityService).findAll();
	}

	@Test
	void getDevuelve200ConLaPrioridadSolicitada() throws Exception {
		when(priorityService.findById(2L)).thenReturn(
				new PriorityResponse(2L, PriorityCode.ALTA, ALTA_NAME, 2, true, new SlaResponse(2, 8)));

		mockMvc.perform(get(BASE_PATH + "/2").with(admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.level").value(2));
		verify(priorityService).findById(2L);
	}

	@Test
	void createDevuelve201ConCabeceraLocation() throws Exception {
		when(priorityService.create(any())).thenReturn(
				new PriorityResponse(3L, PriorityCode.MEDIA, "Media", 3, true, new SlaResponse(4, 24)));

		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON).content(
						"{\"code\":\"MEDIA\",\"name\":\"Media\",\"level\":3,"
								+ "\"sla\":{\"responseHours\":4,\"resolutionHours\":24}}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(BASE_PATH + "/3")))
				.andExpect(jsonPath(CODE_PATH).value("MEDIA"));
		verify(priorityService).create(any());
	}

	@Test
	void createDevuelve409CuandoElCodigoYaExiste() throws Exception {
		when(priorityService.create(any())).thenThrow(
				new ConflictException("PRIORITY_ALREADY_EXISTS", "Ya existe una prioridad con código ALTA"));

		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON).content(
						"{\"code\":\"ALTA\",\"name\":\"Alta\",\"level\":5,"
								+ "\"sla\":{\"responseHours\":1,\"resolutionHours\":2}}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath(CODE_PATH).value("PRIORITY_ALREADY_EXISTS"));
		verify(priorityService).create(any());
	}
}
