package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.security.AdminGuard;
import com.ids.ms_catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.ids.ms_catalog.security.SecurityTestSupport.admin;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class CategoryErrorResponseTest {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String PROBLEM_JSON = "application/problem+json";
	private static final String BASE_PATH = "/api/v1/categories";
	private static final String CODE_PATH = "$.code";

	private final MockMvc mockMvc;

	@MockitoBean
	private AdminGuard adminGuard;

	@MockitoBean
	private CategoryService categoryService;

	@Autowired
	CategoryErrorResponseTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createDevuelve400ConErroresDeValidacion() throws Exception {
		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON).content("{\"name\":\"A\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath(CODE_PATH).value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("name"));
		verify(categoryService, never()).create(any());
	}

	@Test
	void createDevuelve409ConProblemDetailCuandoNombreDuplicado() throws Exception {
		when(categoryService.create(any())).thenThrow(
				new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría llamada \"Software\""));

		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON).content("{\"name\":\"Software\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath(CODE_PATH).value("CATEGORY_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.instance").value(BASE_PATH));
		verify(categoryService).create(any());
	}

	@Test
	void getDevuelve404ConProblemDetailCuandoNoExiste() throws Exception {
		when(categoryService.findById(anyLong())).thenThrow(
				new NotFoundException("CATEGORY_NOT_FOUND", "No existe una categoría con id 999"));

		mockMvc.perform(get(BASE_PATH + "/999").with(admin()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(CODE_PATH).value("CATEGORY_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("No existe una categoría con id 999"))
				.andExpect(jsonPath("$.timestamp").exists());
		verify(categoryService).findById(999L);
	}

	@Test
	void updateDevuelve400CuandoElCuerpoEsIlegible() throws Exception {
		mockMvc.perform(put(BASE_PATH + "/1").with(admin()).contentType(JSON).content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE_PATH).value("MALFORMED_REQUEST"));
		verify(categoryService, never()).update(anyLong(), any());
	}

	@Test
	void errorInesperadoDevuelve500SinExponerDetalleInterno() throws Exception {
		when(categoryService.findAll(anyBoolean())).thenThrow(new IllegalStateException("fallo de conexión interno"));

		mockMvc.perform(get(BASE_PATH).with(admin()).param("active", "false"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath(CODE_PATH).value("INTERNAL_ERROR"))
				.andExpect(content().string(not(containsString("fallo de conexión interno"))));
		verify(categoryService).findAll(false);
	}

	@Test
	void rutaInexistenteDevuelve404ConResourceNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/inexistente").with(admin()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(CODE_PATH).value("RESOURCE_NOT_FOUND"));
		verifyNoInteractions(categoryService);
	}

	@Test
	void metodoNoPermitidoDevuelve405ConProblemDetail() throws Exception {
		mockMvc.perform(patch(BASE_PATH).with(admin()))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath(CODE_PATH).value("METHOD_NOT_ALLOWED"));
		verifyNoInteractions(categoryService);
	}

	@Test
	void tipoDeContenidoNoSoportadoDevuelve415ConProblemDetail() throws Exception {
		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(MediaType.TEXT_PLAIN).content("texto"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath(CODE_PATH).value("UNSUPPORTED_MEDIA_TYPE"));
		verifyNoInteractions(categoryService);
	}
}
