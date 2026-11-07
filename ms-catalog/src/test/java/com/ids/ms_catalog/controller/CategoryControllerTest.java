package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String PROBLEM_JSON = "application/problem+json";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Test
	void createDevuelve201ConCabeceraLocation() throws Exception {
		when(categoryService.create(any())).thenReturn(new CategoryResponse(7L, "Redes", "Problemas de red", true));

		mockMvc.perform(post("/api/v1/categories").contentType(JSON)
						.content("{\"name\":\"Redes\",\"description\":\"Problemas de red\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/categories/7")))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void createDevuelve400ConErroresDeValidacion() throws Exception {
		mockMvc.perform(post("/api/v1/categories").contentType(JSON).content("{\"name\":\"A\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void createDevuelve409ConProblemDetailCuandoNombreDuplicado() throws Exception {
		when(categoryService.create(any())).thenThrow(
				new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría llamada \"Software\""));

		mockMvc.perform(post("/api/v1/categories").contentType(JSON).content("{\"name\":\"Software\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.instance").value("/api/v1/categories"));
	}

	@Test
	void getDevuelve404ConProblemDetailCuandoNoExiste() throws Exception {
		when(categoryService.findById(anyLong())).thenThrow(
				new NotFoundException("CATEGORY_NOT_FOUND", "No existe una categoría con id 999"));

		mockMvc.perform(get("/api/v1/categories/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("No existe una categoría con id 999"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void listPasaElFiltroActiveAlServicio() throws Exception {
		when(categoryService.findAll(true)).thenReturn(
				List.of(new CategoryResponse(2L, "Software", null, true)));

		mockMvc.perform(get("/api/v1/categories").param("active", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Software"));
		verify(categoryService).findAll(true);
	}

	@Test
	void deleteDevuelve204SinCuerpo() throws Exception {
		doNothing().when(categoryService).deactivate(anyLong());

		mockMvc.perform(delete("/api/v1/categories/1"))
				.andExpect(status().isNoContent());
		verify(categoryService).deactivate(1L);
	}

	@Test
	void updateDevuelve400CuandoElCuerpoEsIlegible() throws Exception {
		mockMvc.perform(put("/api/v1/categories/1").contentType(JSON).content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void errorInesperadoDevuelve500SinExponerDetalleInterno() throws Exception {
		when(categoryService.findAll(anyBoolean())).thenThrow(new IllegalStateException("fallo de conexión interno"));

		mockMvc.perform(get("/api/v1/categories").param("active", "false"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(content().string(not(containsString("fallo de conexión interno"))));
	}
}
