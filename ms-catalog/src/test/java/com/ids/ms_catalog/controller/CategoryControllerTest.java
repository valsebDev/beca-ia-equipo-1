package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.config.JwtDecoderConfig;
import com.ids.ms_catalog.config.SecurityConfig;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.security.AdminGuard;
import com.ids.ms_catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ids.ms_catalog.security.SecurityTestSupport.admin;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtDecoderConfig.class})
@ActiveProfiles("test")
class CategoryControllerTest {

	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String BASE_PATH = "/api/v1/categories";

	private final MockMvc mockMvc;

	@MockitoBean
	private AdminGuard adminGuard;

	@MockitoBean
	private CategoryService categoryService;

	@Autowired
	CategoryControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createDevuelve201ConCabeceraLocation() throws Exception {
		when(categoryService.create(any())).thenReturn(new CategoryResponse(7L, "Redes", "Problemas de red", true));

		mockMvc.perform(post(BASE_PATH).with(admin()).contentType(JSON)
						.content("{\"name\":\"Redes\",\"description\":\"Problemas de red\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(BASE_PATH + "/7")))
				.andExpect(jsonPath("$.active").value(true));
		verify(categoryService).create(any());
	}

	@Test
	void listPasaElFiltroActiveAlServicio() throws Exception {
		when(categoryService.findAll(true)).thenReturn(
				List.of(new CategoryResponse(2L, "Software", null, true)));

		mockMvc.perform(get(BASE_PATH).with(admin()).param("active", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Software"));
		verify(categoryService).findAll(true);
	}

	@Test
	void deleteDevuelve204SinCuerpo() throws Exception {
		doNothing().when(categoryService).deactivate(anyLong());

		mockMvc.perform(delete(BASE_PATH + "/1").with(admin()))
				.andExpect(status().isNoContent());
		verify(categoryService).deactivate(1L);
	}
}
