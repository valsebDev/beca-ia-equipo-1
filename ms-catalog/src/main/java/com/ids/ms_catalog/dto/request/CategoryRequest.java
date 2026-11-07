package com.ids.ms_catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank(message = "no debe estar vacío")
		@Size(min = 2, max = 80, message = "debe tener entre 2 y 80 caracteres")
		String name,

		@Size(max = 255, message = "no puede superar 255 caracteres")
		String description) {
}
