package com.ids.ms_catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Límites superiores y orden de las horas se validan en el service con app.sla.* (configurable por entorno).
 */
public record SlaRequest(
		@NotNull(message = "no debe ser nulo")
		@Min(value = 1, message = "debe ser mayor o igual a 1")
		Integer responseHours,

		@NotNull(message = "no debe ser nulo")
		@Min(value = 1, message = "debe ser mayor o igual a 1")
		Integer resolutionHours) {
}
