package com.ids.ms_catalog.service;

import com.ids.ms_catalog.config.properties.SlaProperties;
import com.ids.ms_catalog.dto.request.PriorityRequest;
import com.ids.ms_catalog.dto.request.SlaRequest;
import com.ids.ms_catalog.dto.response.PriorityResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.DomainValidationException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.model.Priority;
import com.ids.ms_catalog.model.PriorityCode;
import com.ids.ms_catalog.model.SlaPolicy;
import com.ids.ms_catalog.repository.PriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriorityServiceTest {

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
	void createLanzaConflictoCuandoElCodigoYaExiste() {
		when(priorityRepository.existsByCode(PriorityCode.ALTA)).thenReturn(true);

		assertThatThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.ALTA, "Alta", 5,
				new SlaRequest(1, 2))))
				.isInstanceOfSatisfying(ConflictException.class,
						ex -> assertThat(ex.getCode()).isEqualTo("PRIORITY_ALREADY_EXISTS"));
		verify(priorityRepository, never()).save(any(Priority.class));
	}

	@Test
	void createRechazaResolucionMenorQueRespuesta() {
		assertThatThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, "Media", 3,
				new SlaRequest(10, 5))))
				.isInstanceOfSatisfying(DomainValidationException.class,
						ex -> assertThat(ex.getField()).isEqualTo("resolutionHours"));
		verify(priorityRepository, never()).save(any(Priority.class));
	}

	@Test
	void createRechazaRespuestaSobreElMaximoConfigurado() {
		assertThatThrownBy(() -> priorityService.create(new PriorityRequest(PriorityCode.MEDIA, "Media", 3,
				new SlaRequest(800, 900))))
				.isInstanceOfSatisfying(DomainValidationException.class,
						ex -> assertThat(ex.getField()).isEqualTo("responseHours"));
	}

	@Test
	void updateSlaActualizaHorasCuandoSonValidas() {
		when(priorityRepository.findByIdWithSla(2L)).thenReturn(Optional.of(alta));

		PriorityResponse response = priorityService.updateSla(2L, new SlaRequest(2, 8));

		assertThat(response.sla().responseHours()).isEqualTo(2);
		assertThat(response.sla().resolutionHours()).isEqualTo(8);
	}

	@Test
	void updateSlaLanzaNotFoundCuandoLaPrioridadNoExiste() {
		when(priorityRepository.findByIdWithSla(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> priorityService.updateSla(999L, new SlaRequest(2, 8)))
				.isInstanceOfSatisfying(NotFoundException.class,
						ex -> assertThat(ex.getCode()).isEqualTo("PRIORITY_NOT_FOUND"));
	}

	@Test
	void findByIdLanzaNotFoundCuandoLaPrioridadNoExiste() {
		when(priorityRepository.findByIdWithSla(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> priorityService.findById(999L)).isInstanceOf(NotFoundException.class);
	}
}
