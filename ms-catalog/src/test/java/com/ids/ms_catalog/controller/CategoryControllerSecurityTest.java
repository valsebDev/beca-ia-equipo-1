package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ids.ms_catalog.security.SecurityTestSupport.jwtWithRole;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class CategoryControllerSecurityTest {

	private static final String BASE_PATH = "/api/v1/categories";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String BODY = "{\"name\":\"Redes\",\"description\":\"Problemas de red\"}";

	private final MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Autowired
	CategoryControllerSecurityTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void lecturaSinTokenDevuelve401() throws Exception {
		mockMvc.perform(get(BASE_PATH))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(categoryService);
	}

	@Test
	void mutacionSinTokenDevuelve401() throws Exception {
		mockMvc.perform(post(BASE_PATH).contentType(JSON).content(BODY))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(categoryService);
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void lecturaPermitidaParaTodosLosRoles(String role) throws Exception {
		when(categoryService.findAll(any())).thenReturn(List.of());

		mockMvc.perform(get(BASE_PATH).with(jwtWithRole(role)))
				.andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void crearCategoriaRequiereAdmin(String role) throws Exception {
		mockMvc.perform(post(BASE_PATH).with(jwtWithRole(role)).contentType(JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verifyNoInteractions(categoryService);
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void actualizarCategoriaRequiereAdmin(String role) throws Exception {
		mockMvc.perform(put(BASE_PATH + "/1").with(jwtWithRole(role)).contentType(JSON).content(BODY))
				.andExpect(status().isForbidden());
		verifyNoInteractions(categoryService);
	}

	@ParameterizedTest
	@ValueSource(strings = {"SOLICITANTE", "AGENTE"})
	void eliminarCategoriaRequiereAdmin(String role) throws Exception {
		mockMvc.perform(delete(BASE_PATH + "/1").with(jwtWithRole(role)))
				.andExpect(status().isForbidden());
		verifyNoInteractions(categoryService);
	}

	@Test
	void eliminarCategoriaSinTokenDevuelve401() throws Exception {
		mockMvc.perform(delete(BASE_PATH + "/1"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(categoryService);
	}

	@Test
	void adminPuedeEliminarCategoria() throws Exception {
		mockMvc.perform(delete(BASE_PATH + "/1").with(jwtWithRole("ADMIN")))
				.andExpect(status().isNoContent());
		verify(categoryService).deactivate(anyLong());
	}
}
