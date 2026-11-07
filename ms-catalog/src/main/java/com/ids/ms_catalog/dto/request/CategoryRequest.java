package com.ids.ms_catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.ids.ms_catalog.model.Category.DESCRIPTION_MAX_LENGTH;
import static com.ids.ms_catalog.model.Category.NAME_MAX_LENGTH;

public record CategoryRequest(
		@NotBlank(message = "no debe estar vacío")
		@Size(min = 2, max = NAME_MAX_LENGTH, message = "debe tener entre 2 y 80 caracteres")
		String name,

		@Size(max = DESCRIPTION_MAX_LENGTH, message = "no puede superar 255 caracteres")
		String description) {
}
