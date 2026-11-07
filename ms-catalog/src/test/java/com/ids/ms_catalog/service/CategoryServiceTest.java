package com.ids.ms_catalog.service;

import com.ids.ms_catalog.dto.request.CategoryRequest;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.Category;
import com.ids.ms_catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	private static final String SOFTWARE_NAME = "Software";

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryService categoryService;

	private Category software;

	@BeforeEach
	void setUp() {
		software = new Category();
		software.setId(2L);
		software.setName(SOFTWARE_NAME);
		software.setDescription("Errores de aplicaciones");
		software.setActive(true);
	}

	@Test
	void createGuardaCategoriaActivaCuandoElNombreEstaDisponible() {
		when(categoryRepository.existsByName("Redes")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
			Category saved = invocation.getArgument(0);
			saved.setId(10L);
			return saved;
		});

		CategoryResponse response = categoryService.create(new CategoryRequest("Redes", "Problemas de red"));

		assertThat(response).usingRecursiveComparison()
				.isEqualTo(new CategoryResponse(10L, "Redes", "Problemas de red", true));
	}

	@Test
	void createLanzaConflictoCuandoElNombreYaExiste() {
		when(categoryRepository.existsByName(SOFTWARE_NAME)).thenReturn(true);

		CategoryRequest request = new CategoryRequest(SOFTWARE_NAME, null);

		assertThatExceptionOfType(ConflictException.class)
				.isThrownBy(() -> categoryService.create(request))
				.extracting(ConflictException::getCode)
				.isEqualTo("CATEGORY_ALREADY_EXISTS");
	}

	@Test
	void createNoGuardaCategoriaCuandoElNombreYaExiste() {
		when(categoryRepository.existsByName(SOFTWARE_NAME)).thenReturn(true);

		CategoryRequest request = new CategoryRequest(SOFTWARE_NAME, null);

		crearEsperandoConflicto(request);

		verify(categoryRepository, never()).save(any(Category.class));
	}

	@Test
	void findAllFiltraPorActivoCuandoSeIndicaElParametro() {
		when(categoryRepository.findAllByActiveOrderByNameAsc(true)).thenReturn(List.of(software));

		List<CategoryResponse> result = categoryService.findAll(true);

		assertThat(result).extracting(CategoryResponse::name).containsExactly(SOFTWARE_NAME);
	}

	@Test
	void findAllDevuelveTodasCuandoNoHayFiltro() {
		when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(software));

		assertThat(categoryService.findAll(null)).hasSize(1);
	}

	@Test
	void findByIdLanzaNotFoundCuandoLaCategoriaNoExiste() {
		when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.findById(999L))
				.isInstanceOfSatisfying(NotFoundException.class,
						ex -> assertThat(ex.getCode()).isEqualTo("CATEGORY_NOT_FOUND"));
	}

	@Test
	void updateLanzaConflictoCuandoElNombrePerteneceAOtraCategoria() {
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(software));
		when(categoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(true);

		CategoryRequest request = new CategoryRequest("Hardware", null);

		assertThatThrownBy(() -> categoryService.update(3L, request))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateActualizaNombreYDescripcionCuandoNoHayConflicto() {
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(software));
		when(categoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);

		CategoryResponse response = categoryService.update(2L, new CategoryRequest("Software y licencias", "Nuevo texto"));

		assertThat(response).usingRecursiveComparison()
				.isEqualTo(new CategoryResponse(2L, "Software y licencias", "Nuevo texto", true));
	}

	private void crearEsperandoConflicto(CategoryRequest request) {
		try {
			categoryService.create(request);
			fail("Se esperaba ConflictException");
		} catch (ConflictException expected) {
			// El tipo y el código de la excepción se verifican en createLanzaConflictoCuandoElNombreYaExiste.
		}
	}

	@Nested
	class Deactivate {

		private void desactivarEsperandoNotFound(long id) {
			try {
				categoryService.deactivate(id);
				fail("Se esperaba NotFoundException");
			} catch (NotFoundException expected) {
				// El tipo de excepción se verifica en deactivateLanzaNotFoundCuandoLaCategoriaNoExiste.
			}
		}

		@Test
		void deactivateMarcaInactivaSinBorrarElRegistro() {
			when(categoryRepository.findById(2L)).thenReturn(Optional.of(software));

			categoryService.deactivate(2L);

			assertThat(software.isActive()).isFalse();
		}

		@Test
		void deactivateNoLlamaDeleteDelRepositorio() {
			when(categoryRepository.findById(2L)).thenReturn(Optional.of(software));

			categoryService.deactivate(2L);

			verify(categoryRepository, never()).delete(any(Category.class));
		}

		@Test
		void deactivateNoBorraElRegistroFisicamente() {
			when(categoryRepository.findById(2L)).thenReturn(Optional.of(software));

			categoryService.deactivate(2L);

			verify(categoryRepository, never()).deleteById(anyLong());
		}

		@Test
		void deactivateLanzaNotFoundCuandoLaCategoriaNoExiste() {
			when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> categoryService.deactivate(999L)).isInstanceOf(NotFoundException.class);
		}

		@Test
		void deactivateNoLlamaDeleteIdCuandoLaCategoriaNoExiste() {
			when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

			desactivarEsperandoNotFound(999L);

			verify(categoryRepository, never()).deleteById(anyLong());
		}
	}
}
