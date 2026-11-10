package com.ids.ms_catalog.dto.request;

import com.ids.ms_catalog.model.PriorityCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.ids.ms_catalog.model.Priority.LEVEL_MAX;
import static com.ids.ms_catalog.model.Priority.NAME_MAX_LENGTH;

public record PriorityRequest(
		@NotNull(message = "no debe ser nulo")
		PriorityCode code,

		@NotBlank(message = "no debe estar vacío")
		@Size(max = NAME_MAX_LENGTH, message = "no puede superar 50 caracteres")
		String name,

		@NotNull(message = "no debe ser nulo")
		@Min(value = 1, message = "debe ser mayor o igual a 1")
		@Max(value = LEVEL_MAX, message = "debe ser menor o igual a 10")
		Integer level,

		@NotNull(message = "no debe ser nulo")
		@Valid
		SlaRequest sla) {
}
