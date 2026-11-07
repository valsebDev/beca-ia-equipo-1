package com.ids.ms_catalog.service;

import com.ids.ms_catalog.config.properties.SlaProperties;
import com.ids.ms_catalog.dto.request.PriorityRequest;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.dto.response.SlaResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.DomainValidationException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.Priority;
import com.ids.ms_catalog.model.PriorityCode;
import com.ids.ms_catalog.model.SlaPolicy;
import com.ids.ms_catalog.repository.PriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriorityServiceTest {

	private static final String MEDIA_NAME = "Media";

	@Mock
	private PriorityRepository priorityRepository;

	private PriorityService priorityService;
	private Priority alta;

	@BeforeEach
	void setUp() {
		priorityService = new PriorityService(priorityRepository, new SlaProperties(720, 2160));
		alta = new Priority();
		alta.setId(2L);
		alta.setCode(PriorityCode.ALTA);
		alta.setName("Alta");
		alta.setLevel((short) 2);
		alta.setActive(true);
		SlaPolicy sla = new SlaPolicy();
		sla.setResponseHours(4);
		sla.setResolutionHours(24);
		sla.setPriority(alta);
		alta.setSla(sla);
	}

	@Test
	void findAllDevuelvePrioridadesConSuSla() {
		when(priorityRepository.findAllWithSlaOrderByLevel()).thenReturn(List.of(alta));

		assertThat(priorityService.findAll()).extracting(PriorityResponse::code).containsExactly(PriorityCode.ALTA);
	}

	@Test
	void findByIdDevuelveLaPrioridadSolicitada() {
		when(priorityRepository.findByIdWithSla(2L)).thenReturn(Optional.of(alta));

		assertThat(priorityService.findById(2L).name()).isEqualTo("Alta");
	}

	@Test
	void findByIdLanzaNotFoundCuandoLaPrioridadNoExiste() {
		when(priorityRepository.findByIdWithSla(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> priorityService.findById(999L)).isInstanceOf(NotFoundException.class);
	}

	@Nested
	class Crear {

		@Test
		void createGuardaPrioridadCuandoCodigoYNivelEstanDisponibles() {
			when(priorityRepository.existsByCode(PriorityCode.MEDIA)).thenReturn(false);
			when(priorityRepository.existsByLevel((short) 3)).thenReturn(false);
			when(priorityRepository.save(any(Priority.class))).thenAnswer(invocation -> {
				Priority saved = invocation.getArgument(0);
				saved.setId(5L);
				return saved;
			});

			PriorityResponse response = priorityService.create(new PriorityRequest(PriorityCode.MEDIA, MEDIA_NAME, 3,
					new SlaRequest(4, 24)));

			assertThat(response).usingRecursiveComparison()
					.isEqualTo(new PriorityResponse(5L, PriorityCode.MEDIA, MEDIA_NAME, 3, true, new SlaResponse(4, 24)));
		}

		@Test
		void createLanzaConflictoCuandoElCodigoYaExiste() {
			when(priorityRepository.existsByCode(PriorityCode.ALTA)).thenReturn(true);

			assertThatExceptionOfType(ConflictException.class)
					.isThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.ALTA, "Alta", 5,
							new SlaRequest(1, 2))))
					.extracting(ConflictException::getCode)
					.isEqualTo("PRIORITY_ALREADY_EXISTS");
		}

		@Test
		void createLanzaConflictoCuandoElNivelYaExiste() {
			when(priorityRepository.existsByCode(PriorityCode.MEDIA)).thenReturn(false);
			when(priorityRepository.existsByLevel((short) 3)).thenReturn(true);

			assertThatExceptionOfType(ConflictException.class)
					.isThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, MEDIA_NAME, 3,
							new SlaRequest(4, 24))))
					.extracting(ConflictException::getCode)
					.isEqualTo("PRIORITY_LEVEL_ALREADY_EXISTS");
		}

		@Test
		void createRechazaResolucionMenorQueRespuesta() {
			assertThatExceptionOfType(DomainValidationException.class)
					.isThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, MEDIA_NAME, 3,
							new SlaRequest(10, 5))))
					.extracting(DomainValidationException::getField)
					.isEqualTo("resolutionHours");
		}

		@Test
		void createRechazaRespuestaSobreElMaximoConfigurado() {
			assertThatExceptionOfType(DomainValidationException.class)
					.isThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, MEDIA_NAME, 3,
							new SlaRequest(800, 900))))
					.extracting(DomainValidationException::getField)
					.isEqualTo("responseHours");
		}
	}

	@Nested
	class ActualizarSla {

		@Test
		void updateSlaActualizaHorasCuandoSonValidas() {
			when(priorityRepository.findByIdWithSla(2L)).thenReturn(Optional.of(alta));

			PriorityResponse response = priorityService.updateSla(2L, new SlaRequest(2, 8));

			assertThat(response.sla()).isEqualTo(new SlaResponse(2, 8));
		}

		@Test
		void updateSlaLanzaNotFoundCuandoLaPrioridadNoExiste() {
			when(priorityRepository.findByIdWithSla(anyLong())).thenReturn(Optional.empty());

			assertThatExceptionOfType(NotFoundException.class)
					.isThrownBy(() -> priorityService.updateSla(999L, new SlaRequest(2, 8)))
					.extracting(NotFoundException::getCode)
					.isEqualTo("PRIORITY_NOT_FOUND");
		}

		@Test
		void updateSlaRechazaResolucionMenorQueRespuesta() {
			when(priorityRepository.findByIdWithSla(2L)).thenReturn(Optional.of(alta));

			assertThatExceptionOfType(DomainValidationException.class)
					.isThrownBy(() -> priorityService.updateSla(2L, new SlaRequest(10, 5)))
					.extracting(DomainValidationException::getField)
					.isEqualTo("resolutionHours");
		}

		@Test
		void updateSlaRechazaResolucionSobreElMaximoConfigurado() {
			when(priorityRepository.findByIdWithSla(2L)).thenReturn(Optional.of(alta));

			assertThatExceptionOfType(DomainValidationException.class)
					.isThrownBy(() -> priorityService.updateSla(2L, new SlaRequest(1, 3000)))
					.extracting(DomainValidationException::getField)
					.isEqualTo("resolutionHours");
		}
	}
}
